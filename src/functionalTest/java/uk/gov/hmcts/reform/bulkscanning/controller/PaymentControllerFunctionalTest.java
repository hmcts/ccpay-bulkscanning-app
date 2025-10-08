package uk.gov.hmcts.reform.bulkscanning.controller;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.bulkscanning.config.BulkScanPaymentTestService;
import uk.gov.hmcts.reform.bulkscanning.config.IdamService;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.config.User;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
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
@SuppressWarnings({
    "PMD.JUnitTestsShouldIncludeAssert",
    "PMD.UseLocaleWithCaseConversions",
    "PMD.LiteralsFirstInComparisons"
})
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
    private static final int CCD_EIGHT_DIGIT_UPPER = 99_999_999;
    private static final int CCD_EIGHT_DIGIT_LOWER = 10_000_000;
    private static String userEmail;
    private final List<String> dcns = new ArrayList<>();

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
    public void testBulkScanningPaymentRequestAndMarkPaymentAsProcessed() {
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        Response unprocessedPaymentDetailsByDcnResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0]
        );
        unprocessedPaymentDetailsByDcnResponse1.then().statusCode(OK.value())
            .body("all_payments_status", is(equalTo("INCOMPLETE")));

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn))
        );

        Response unprocessedPaymentDetailsByDcnResponse2 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0]
        );
        unprocessedPaymentDetailsByDcnResponse2.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDcnPayment.getBankGiroCreditSlipNumber().toString())))
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        //PATCH Request
        Response patchResp = bulkScanPaymentTestService.updateBulkScanPaymentStatus(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0],
            "PROCESSED"
        );
        patchResp.then().statusCode(OK.value());
    }

    @Test
    public void testNegativeDuplicateBulkScanChequePayment() {
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPayment bulkScanDcnPayment1 = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse1 = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment1
        );
        bulkScanDcnPaymentResponse1.then().statusCode(CONFLICT.value()).and().toString().equals(
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST);
    }

    @Test
    public void testNegativeDuplicateBulkScanCcdPayment() {
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn))
        );

        BulkScanPaymentRequest bulkScanCcdPayments1 = createBulkScanCcdPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse1 = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments1
        );
        bulkScanCcdPaymentsResponse1.then().statusCode(CONFLICT.value()).and().toString().equals(
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST);
    }

    @Test
    public void testNegativeDcnNotExistsForBulkScanPaymentProcess() {
        String[] dcn = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        Response updateBulkScanPaymentStatusResponse = bulkScanPaymentTestService.updateBulkScanPaymentStatus(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0],
            "PROCESSED"
        );
        updateBulkScanPaymentStatusResponse.then().statusCode(NOT_FOUND.value()).and().toString().equals(DCN_NOT_EXISTS);
    }

    @Test
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void testUpdateCaseReferenceForMultipleEnvelopesExceptionRecordAndGetDetailsByDcns() {
        String[] dcn1 = {"6600000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn1[0]);
        String[] dcn2 = {"6600000000002" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn2[0]);

        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        BulkScanPayment bulkScanDcnPayment1 = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn1[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse1 = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment1
        );
        bulkScanDcnPaymentResponse1.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPayment bulkScanDcnPayment2 = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn2[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse2 = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment2
        );
        bulkScanDcnPaymentResponse2.then().statusCode(CREATED.value()).and().toString().equals("created");

        //Multiple envelopes with same exception record
        BulkScanPaymentRequest bulkScanCcdPayments1 = createBulkScanCcdPayments(exceptionReference, dcn1, "AA08", true);
        Response bulkScanCcdPaymentsResponse1 = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments1
        );
        bulkScanCcdPaymentsResponse1.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn1))
        );

        BulkScanPaymentRequest bulkScanCcdPayments2 = createBulkScanCcdPayments(exceptionReference, dcn2, "AA08", true);
        Response bulkScanCcdPaymentsResponse2 = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments2
        );
        bulkScanCcdPaymentsResponse2.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn2))
        );

        // verify exception ref payment details by dcn1
        Response unprocessedPaymentDetailsByDcnResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn1[0]
        );
        unprocessedPaymentDetailsByDcnResponse1.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments1.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body(
                "payments[0].bgc_reference",
                is(equalTo(bulkScanDcnPayment1.getBankGiroCreditSlipNumber().toString()))
            )
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment1.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment1.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment1.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // verify exception ref payment details by dcn2
        Response unprocessedPaymentDetailsByDcnResponse2 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn2[0]
        );
        unprocessedPaymentDetailsByDcnResponse2.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments2.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn2[0])))
            .body(
                "payments[0].bgc_reference",
                is(equalTo(bulkScanDcnPayment2.getBankGiroCreditSlipNumber().toString()))
            )
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment2.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment2.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment2.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(
            SERVICE_TOKEN,
            exceptionReference,
            caseReferenceRequest
        );
        response.then().statusCode(OK.value());
        Assert.assertNotNull(response.andReturn().asString());

        // verify ccd case ref payment details by dcn1
        Response unprocessedPaymentDetailsByDcnResponse3 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn1[0]
        );
        unprocessedPaymentDetailsByDcnResponse3.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments1.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn1[0])))
            .body(
                "payments[0].bgc_reference",
                is(equalTo(bulkScanDcnPayment1.getBankGiroCreditSlipNumber().toString()))
            )
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment1.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment1.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment1.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // verify ccd case ref payment details by dcn2
        Response unprocessedPaymentDetailsByDcnResponse4 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn2[0]
        );
        unprocessedPaymentDetailsByDcnResponse4.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments2.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn2[0])))
            .body(
                "payments[0].bgc_reference",
                is(equalTo(bulkScanDcnPayment2.getBankGiroCreditSlipNumber().toString()))
            )
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment2.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment2.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment2.getMethod().toUpperCase())))
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

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn))
        );

        Response unprocessedPaymentDetailsByDcnResponse1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0]
        );
        unprocessedPaymentDetailsByDcnResponse1.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDcnPayment.getBankGiroCreditSlipNumber().toString())))
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        // Seems like duplicate endpoints on controller, not much different to the above one.
        Response unprocessedPaymentDetailsByDcnResponse2 = bulkScanPaymentTestService.getCasesUnprocessedPaymentDetailsByDcn(
            USER_TOKEN,
            SERVICE_TOKEN,
            dcn[0]
        );
        unprocessedPaymentDetailsByDcnResponse2.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDcnPayment.getBankGiroCreditSlipNumber().toString())))
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment.getMethod().toUpperCase())))
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

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn))
        );

        Response response1 = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByCcdOrExceptionCaseReference(
            USER_TOKEN,
            SERVICE_TOKEN,
            ccdCaseNumber
        );
        response1.then().statusCode(OK.value());
    }

    @Test
    public void testUnprocessedPaymentDetailsWithExceptionAndCcdCaseReference() {
        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String[] dcn = {"6200000000001" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER)};
        dcns.add(dcn[0]);

        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcn[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(exceptionReference, dcn, "AA08", true);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcn))
        );

        Response exceptionCaseReferencePaymentDetailsResponse = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByCcdOrExceptionCaseReference(
            USER_TOKEN,
            SERVICE_TOKEN,
            exceptionReference
        );
        exceptionCaseReferencePaymentDetailsResponse.then().statusCode(OK.value())
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDcnPayment.getBankGiroCreditSlipNumber().toString())))
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));

        String ccdCaseNumber = "13115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(
            SERVICE_TOKEN,
            exceptionReference,
            caseReferenceRequest
        );
        response.then().statusCode(OK.value());

        Response ccdCaseReferencePaymentDetailsResponse = bulkScanPaymentTestService.getUnprocessedPaymentDetailsByCcdOrExceptionCaseReference(
            USER_TOKEN,
            SERVICE_TOKEN,
            ccdCaseNumber
        );
        ccdCaseReferencePaymentDetailsResponse.then().statusCode(OK.value())
            .body("ccd_reference", is(equalTo(ccdCaseNumber)))
            .body("exception_record_reference", is(equalTo(exceptionReference)))
            .body("responsible_service_id", is(equalTo(bulkScanCcdPayments.getResponsibleServiceId())))
            .body("payments[0].id", notNullValue())
            .body("payments[0].dcn_reference", is(equalTo(dcn[0])))
            .body("payments[0].bgc_reference", is(equalTo(bulkScanDcnPayment.getBankGiroCreditSlipNumber().toString())))
            .body(
                "payments[0].amount",
                is(equalTo(new BigDecimal(String.valueOf(bulkScanDcnPayment.getAmount())).setScale(
                    2,
                    RoundingMode.HALF_UP
                )))
            )
            .body("payments[0].currency", is(equalTo(bulkScanDcnPayment.getCurrency())))
            .body("payments[0].payment_method", is(equalTo(bulkScanDcnPayment.getMethod().toUpperCase())))
            .body("payments[0].date_banked", notNullValue())
            .body("payments[0].date_created", notNullValue())
            .body("payments[0].date_updated", notNullValue())
            .body("all_payments_status", is(equalTo("COMPLETE")));
    }

    @Test
    public void testExceptionRecordNotExists() {
        String exceptionReference = "11223344" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);
        String ccdCaseNumber = "11115656" + RandomUtils.nextInt(CCD_EIGHT_DIGIT_LOWER, CCD_EIGHT_DIGIT_UPPER);

        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber(ccdCaseNumber)
            .build();

        Response response = bulkScanPaymentTestService.updateCaseReferenceForExceptionReference(
            SERVICE_TOKEN,
            exceptionReference,
            caseReferenceRequest
        );
        response.then().statusCode(NOT_FOUND.value());
        Assert.assertTrue(StringUtils.containsIgnoreCase(
            response.andReturn().asString(),
            EXCEPTION_RECORD_NOT_EXISTS
        ));
    }

    @Test
    public void testGeneratePaymentReport_Unprocessed() {
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
    public void testGeneratePaymentReport_DataLoss() {
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

    public static BulkScanPayment createBulkScanDcnPayment(BigDecimal amount, Integer bankGiroCreditSlipNumber, String bankedDate,
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

    public static BulkScanPaymentRequest createBulkScanCcdPayments(String ccdCaseNumber, String[] dcn,
                                                                   String responsibleServiceId, boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    private void createTestReportData(String ccdCaseNumber, String... dcns) {
        //Request from bulk scan provider with one Dcn
        BulkScanPayment bulkScanDcnPayment = createBulkScanDcnPayment(
            new BigDecimal(273),
            964_567,
            LocalDate.now().toString(),
            "GBP",
            dcns[0],
            "cheque"
        );
        Response bulkScanDcnPaymentResponse = bulkScanPaymentTestService.postBulkScanDcnPayment(
            SERVICE_TOKEN,
            bulkScanDcnPayment
        );
        bulkScanDcnPaymentResponse.then().statusCode(CREATED.value()).and().toString().equals("created");

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcns, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(
            SERVICE_TOKEN,
            bulkScanCcdPayments
        );
        bulkScanCcdPaymentsResponse.then().statusCode(CREATED.value()).body(
            "payment_dcns",
            equalTo(Arrays.asList(dcns))
        );
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(
            reportNameDateFormat);
    }

    @After
    public void deleteDcnPayment() {
        if (!dcns.isEmpty()) {
            dcns.forEach(dcn -> bulkScanPaymentTestService.deleteDcnPayment(USER_TOKEN, SERVICE_TOKEN, dcn));
        }
    }

    @SuppressWarnings("PMD.JUnit4TestShouldUseAfterAnnotation")
    @AfterClass
    public static void tearDown() {
        IdamService.deleteUser(userEmail);
    }

}
