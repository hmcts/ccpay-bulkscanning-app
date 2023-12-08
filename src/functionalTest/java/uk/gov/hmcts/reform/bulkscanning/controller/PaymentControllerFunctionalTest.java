package uk.gov.hmcts.reform.bulkscanning.controller;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.bulkscanning.config.*;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;
import static uk.gov.hmcts.reform.bulkscanning.config.IdamService.CMC_CITIZEN_GROUP;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.DCN_NOT_EXISTS;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.EXCEPTION_RECORD_NOT_EXISTS;


@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@EnableFeignClients
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class PaymentControllerFunctionalTest {

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private BulkScanPaymentTestService bulkScanPaymentTestService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED;
    private static final int CCD_EIGHT_DIGIT_UPPER = 99999999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10000000;
    private static String userEmail;
    private List<String> dcns = new ArrayList<>();

    @Before
    public void setUp() {
        if (!TOKENS_INITIALIZED) {
            User user = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen");
            userEmail = user.getEmail();
            USER_TOKEN = user.getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void testBulkScanningPaymentRequestAndMarkPaymentAsProcessed() throws Exception {
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        Response unprocessedPaymentDetailsByDCNResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn[0]);
        unprocessedPaymentDetailsByDCNResponse1.then().statusCode(OK.value())
            .body("all_payments_status", is(equalTo("INCOMPLETE")));

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn)));

        Response unprocessedPaymentDetailsByDCNResponse2 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn[0]);
        unprocessedPaymentDetailsByDCNResponse2.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        //PATCH Request
        Response patchResp = bulkScanPaymentTestService.updateBulkScanPaymentStatus(USER_TOKEN, SERVICE_TOKEN, dcn[0], "PROCESSED");
        patchResp.then().statusCode(OK.value());
    }

    @Test
    public void testNegativeDuplicateBulkScanChequePayment() {
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPayment bulkScanDCNPayment1 = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse1 = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment1);
        bulkScanDCNPaymentResponse1.then().statusCode(CONFLICT.value()).and().toString().equals(BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST);
    }

    @Test
    public void testNegativeDuplicateBulkScanCcdPayment() {
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn)));

        BulkScanPaymentRequest bulkScanCCDPayments1 = createBulkScanCCDPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCCDPaymentsResponse1 = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments1);
        bulkScanCCDPaymentsResponse1.then().statusCode(CONFLICT.value()).and().toString().equals(BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST);
    }

    @Test
    public void testNegativeDcnNotExistsForBulkScanPaymentProcess(){
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        Response updateBulkScanPaymentStatusResponse = bulkScanPaymentTestService.updateBulkScanPaymentStatus(USER_TOKEN, SERVICE_TOKEN, dcn[0], "PROCESSED");
        updateBulkScanPaymentStatusResponse.then().statusCode(NOT_FOUND.value()).and().toString().equals(DCN_NOT_EXISTS);
    }

    @Test
    public void testUpdateCaseReferenceForMultipleEnvelopesExceptionRecordAndGetDetailsByDCNs() throws Exception {
        String[] dcn1 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn1[0]);
        String[] dcn2 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn2[0]);

        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        BulkScanPayment bulkScanDCNPayment1 = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn1[0], "cheque");
        Response bulkScanDCNPaymentResponse1 = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment1);
        bulkScanDCNPaymentResponse1.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPayment bulkScanDCNPayment2 = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn2[0], "cheque");
        Response bulkScanDCNPaymentResponse2 = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment2);
        bulkScanDCNPaymentResponse2.then().statusCode(CREATED.value()).and().toString().equals("created");

        //Multiple envelopes with same exception record
        BulkScanPaymentRequest bulkScanCCDPayments1 = createBulkScanCCDPayments(exceptionReference, dcn1, "AA08", true);
        Response bulkScanCCDPaymentsResponse1 = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments1);
        bulkScanCCDPaymentsResponse1.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn1)));

        BulkScanPaymentRequest bulkScanCCDPayments2 = createBulkScanCCDPayments(exceptionReference, dcn2, "AA08", true);
        Response bulkScanCCDPaymentsResponse2 = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments2);
        bulkScanCCDPaymentsResponse2.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn2)));

        // verify exception ref payment details by dcn1
        Response unprocessedPaymentDetailsByDCNResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn1[0]);
        unprocessedPaymentDetailsByDCNResponse1.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments1.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment1.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment1.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment1.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment1.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // verify exception ref payment details by dcn2
        Response unprocessedPaymentDetailsByDCNResponse2 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn2[0]);
        unprocessedPaymentDetailsByDCNResponse2.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments2.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment2.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment2.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment2.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment2.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(SERVICE_TOKEN, exceptionReference, caseReferenceRequest);
        response.then().statusCode(OK.value());
        Assert.assertNotNull(response.andReturn().asString());

        // verify ccd case ref payment details by dcn1
        Response unprocessedPaymentDetailsByDCNResponse3 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn1[0]);
        unprocessedPaymentDetailsByDCNResponse3.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments1.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment1.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment1.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment1.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment1.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // verify ccd case ref payment details by dcn2
        Response unprocessedPaymentDetailsByDCNResponse4 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn2[0]);
        unprocessedPaymentDetailsByDCNResponse4.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments2.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment2.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment2.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment2.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment2.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));
    }

    @Test
    public void testUnprocessedPaymentDetailsWithDcn() {
        String ccdCaseNumber = "12115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6100000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn)));

        Response unprocessedPaymentDetailsByDCNResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn[0]);
        unprocessedPaymentDetailsByDCNResponse1.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // Seems like duplicate endpoints on controller, not much different to the above one.
        Response unprocessedPaymentDetailsByDCNResponse2 = bulkScanPaymentTestService.getCasesUnprocessedPaymentDetailsByDCN(USER_TOKEN, SERVICE_TOKEN, dcn[0]);
        unprocessedPaymentDetailsByDCNResponse2.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment.getAmount())).setScale(
                2,
                RoundingMode.HALF_UP
            ))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));
    }

    @Test
    public void testUnprocessedPaymentDetailsWithCcdCaseReference() {
        String ccdCaseNumber = "13115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6200000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn)));

        Response response1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByccdOrExceptionCaseReference(USER_TOKEN, SERVICE_TOKEN, ccdCaseNumber);
        response1.then().statusCode(OK.value());
    }

    @Test
    public void testUnprocessedPaymentDetailsWithExceptionAndCcdCaseReference() {
        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String ccdCaseNumber = "13115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6200000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcn[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(exceptionReference, dcn, "AA08", true);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcn)));

        Response exceptionCaseReferencePaymentDetailsResponse = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByccdOrExceptionCaseReference(USER_TOKEN, SERVICE_TOKEN, exceptionReference);
        exceptionCaseReferencePaymentDetailsResponse.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment.getAmount())).setScale(2, BigDecimal.ROUND_HALF_UP))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(SERVICE_TOKEN, exceptionReference, caseReferenceRequest);
        response.then().statusCode(OK.value());

        Response ccdCaseReferencePaymentDetailsResponse = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByccdOrExceptionCaseReference(USER_TOKEN, SERVICE_TOKEN, ccdCaseNumber);
        ccdCaseReferencePaymentDetailsResponse.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCCDPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDCNPayment.getBankGiroCreditSlipNumber().toString())))
            .body("payments[0].amount", is(equalTo(new BigDecimal(String.valueOf(bulkScanDCNPayment.getAmount())).setScale(2, BigDecimal.ROUND_HALF_UP))))
            .body("payments[0].currency", is(equalTo(bulkScanDCNPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDCNPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));
    }

    @Test
    public void testExceptionRecordNotExists() throws Exception {
        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(SERVICE_TOKEN, exceptionReference, caseReferenceRequest);
        response.then().statusCode(NOT_FOUND.value());
        Assert.assertTrue(StringUtils.containsIgnoreCase(
            response.andReturn().asString(),
            EXCEPTION_RECORD_NOT_EXISTS
        ));
    }

    @Test
    public void testGeneratePaymentReport_Unprocessed() throws Exception {
        String[] dcn1 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn1[0]);
        String[] dcn2 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn2[0]);

        String[] dcn = {dcn1[0], dcn2[0]};
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        createTestReportData(ccdCaseNumber, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");

        Response response1 = bulkScanPaymentTestService.retrieveReportData(USER_TOKEN, SERVICE_TOKEN, params);
        response1.then().statusCode(OK.value());

        Response response2 = bulkScanPaymentTestService.downloadReport(USER_TOKEN, SERVICE_TOKEN, params);
        response2.then().statusCode(OK.value());
    }

    @Test
    public void testGeneratePaymentReport_DataLoss() throws Exception {
        String[] dcn1 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn1[0]);
        String[] dcn2 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn2[0]);

        String[] dcn = {dcn1[0], dcn2[0]};
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        createTestReportData(ccdCaseNumber, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");

        Response response1 = bulkScanPaymentTestService.retrieveReportData(USER_TOKEN, SERVICE_TOKEN, params);
        response1.then().statusCode(OK.value());

        Response response2 = bulkScanPaymentTestService.downloadReport(USER_TOKEN, SERVICE_TOKEN, params);
        response2.then().statusCode(OK.value());
    }

    public static BulkScanPayment createBulkScanDCNPayment(BigDecimal amount, Integer bankGiroCreditSlipNumber, String bankedDate,
                                                           String currency, String dcnReference, String method) {

        return BulkScanPayment
            .createPaymentRequestWith()
            .amount(amount)
            .bankGiroCreditSlipNumber(bankGiroCreditSlipNumber)
            .bankedDate(bankedDate)
            .currency(currency)
            .dcnReference(dcnReference)
            .method(method)
            .build();
    }

    public static BulkScanPaymentRequest createBulkScanCCDPayments(String ccdCaseNumber, String[] dcn,
                                                                   String responsibleServiceId, boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    private void createTestReportData(String ccdCaseNumber, String... dcns) throws Exception {
        //Request from bulk scan provider with one DCN
        BulkScanPayment bulkScanDCNPayment = createBulkScanDCNPayment(new BigDecimal(273), 964567, LocalDate.now().toString(), "GBP", dcns[0], "cheque");
        Response bulkScanDCNPaymentResponse = bulkScanPaymentTestService.postBulkScanDCNPayment(SERVICE_TOKEN, bulkScanDCNPayment);
        bulkScanDCNPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCCDPayments = createBulkScanCCDPayments(ccdCaseNumber, dcns, "AA08", false);
        Response bulkScanCCDPaymentsResponse = bulkScanPaymentTestService.postBulkScanCCDPayments(SERVICE_TOKEN, bulkScanCCDPayments);
        bulkScanCCDPaymentsResponse.then().statusCode(CREATED.value()).body("payment_dcns", equalTo(Arrays.asList(dcns)));
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(reportNameDateFormat);
    }

    @After
    public void deleteDcnPayment() {
        if(!dcns.isEmpty()){
            dcns.forEach((dcn) -> bulkScanPaymentTestService.deleteDCNPayment(USER_TOKEN, SERVICE_TOKEN, dcn));
        }
    }

    @AfterClass
    public static void tearDown() {
        IdamService.deleteUser(userEmail);
    }

}
