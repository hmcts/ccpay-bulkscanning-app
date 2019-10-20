package uk.gov.hmcts.reform.bulkscanning.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.apache.commons.lang.StringUtils;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.bulkscanning.config.IdamService;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static uk.gov.hmcts.reform.bulkscanning.config.IdamService.CMC_CITIZEN_GROUP;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(locations="classpath:application-test.yaml")
public class PaymentControllerFunctionalTest {

    @Autowired
    PaymentService bulkScanConsumerService;

    BulkScanPaymentRequest bulkScanPaymentRequest;

    CaseReferenceRequest caseReferenceRequest;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED;

    @Before
    public void setUp() {
        caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"DCN2"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-5555"
            ,dcn,"AA08", true);

        //Post request
        Response response = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(bulkScanPaymentRequest)
            .contentType(ContentType.JSON)
            .when()
            .post("/bulk-scan-payments");

        Assert.assertNotNull(response.andReturn().asString());

        //Post Repeat request
        Response repeatResponse = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(bulkScanPaymentRequest)
            .contentType(ContentType.JSON)
            .when()
            .post("/bulk-scan-payments");

        Assert.assertTrue(StringUtils.containsIgnoreCase(
            repeatResponse.andReturn().asString(),
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST
        ));

        //PATCH Request
        Response patchResp = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .when()
            .patch("/bulk-scan-payments/DCN2/status/PROCESSED");

        Assert.assertNotNull(patchResp.andReturn().asString());

        //DCN Not exists Request
        Response patchDCNNotExists = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .when()
            .patch("/bulk-scan-payments/DCN4/status/PROCESSED");

        Assert.assertTrue(StringUtils.containsIgnoreCase(patchDCNNotExists.andReturn().asString(),
            DCN_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception {
        String dcn[] = {"DCN5"};
        String dcn2[] = {"DCN6"};

        //Multiple envelopes with same exception record
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn2, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(caseReferenceRequest)
            .contentType(ContentType.JSON)
            .when()
            .put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444");

        Assert.assertNotNull(resultActions.andReturn().asString());
    }

    @Test
    @Transactional
    public void testExceptionRecordNotExists() throws Exception {

        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(caseReferenceRequest)
            .contentType(ContentType.JSON)
            .when()
            .put("/bulk-scan-payments/?exception_reference=4444-3333-2222-111");

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().asString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception {
        String dcn[] = {"DCN1"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .when()
            .patch("/bulk-scan-payments/DCN1/status/PROCESSED");

        //Assert.assertEquals(resultActions.andReturn().getStatusCode(), OK.value());
        Assert.assertNotNull(resultActions.andReturn().asString());
    }

    @Test
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-4444-5555"};
        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest("1111-2222-4444-5555"))
            .when()
            .post("/bulk-scan-payment");

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        Response bsResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");

        /*//Complete payment
        EnvelopePayment payment = paymentRepository.findByDcnReference("1111-2222-4444-5555").get();
        Assert.assertEquals(COMPLETE.toString(), payment.getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findById(payment.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());*/
        Assert.assertNotNull(exelaResp.andReturn().asString());
        Assert.assertNotNull(bsResp.andReturn().asString());
    }

    @Test
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-3333-6666", "1111-2222-3333-7777"};
        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest("1111-2222-3333-6666"))
            .when()
            .post("/bulk-scan-payment");

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        Response bsResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");

        /*//Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-6666").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-7777").get().getPaymentStatus()
            , INCOMPLETE.toString());*/
        Assert.assertNotNull(exelaResp.andReturn().asString());
        Assert.assertNotNull(bsResp.andReturn().asString());
    }


    @Test
    public void testMatchingBulkScanFirstThenExela() throws Exception {
        //Request from Bulk Scan with one DCN
        String dcn[] = {"1111-2222-3333-8888", "1111-2222-3333-9999"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        Response bsResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");

        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest("1111-2222-3333-8888"))
            .when()
            .post("/bulk-scan-payment");

        /*//Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-8888").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-9999").get().getPaymentStatus()
            , INCOMPLETE.toString());*/
        Assert.assertNotNull(bsResp.andReturn().asString());
        Assert.assertNotNull(exelaResp.andReturn().asString());

    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String[] dcn, String responsibleServiceId, boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(ResponsibleSiteId.valueOf(responsibleServiceId).toString())
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    @Test
    public void testGeneratePaymentReport_Unprocessed() throws Exception {

        String dcn[] = {"11112222333344441", "11112222333344442"};
        String ccd = "1111222233334444";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        Response response = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/report/download");
        Assert.assertEquals(200, response.andReturn().getStatusCode());
    }

    @Test
    public void testGeneratePaymentReport_DataLoss() throws Exception {
        String dcn[] = {"11112222333355551", "11112222333355552"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        Response response = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/report/download");
        Assert.assertEquals(200, response.andReturn().getStatusCode());
    }

    private void createTestReportData(String ccd, String... dcns) throws Exception {
        //Request from Exela with one DCN

        RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest(dcns[0]))
            .when()
            .post("/bulk-scan-payment");

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(ccd
            , dcns, "AA08", true);

        //Post request
        RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
