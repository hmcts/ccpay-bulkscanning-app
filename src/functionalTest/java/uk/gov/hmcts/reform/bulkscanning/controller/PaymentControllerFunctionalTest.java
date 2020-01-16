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
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
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
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.DCN_NOT_EXISTS;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.EXCEPTION_RECORD_NOT_EXISTS;


@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
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
    PaymentMetadataRepository paymentMetadataRepository;

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
            .ccdCaseNumber("9982111111111111")
            .build();

        if (!TOKENS_INITIALIZED) {
            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test
    public void testBulkScanningPaymentRequestFirst() throws Exception {
        String[] dcn = {"987211111111111111111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233335555",
                                                                                     dcn, "AA08", true);

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
            .patch("/bulk-scan-payments/987211111111111111111/status/PROCESSED");

        Assert.assertNotNull(patchResp.andReturn().asString());

        //DCN Not exists Request
        Response patchdcnnotexists = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .when()
            .patch("/bulk-scan-payments/987411111111111111111/status/PROCESSED");

        Assert.assertTrue(StringUtils.containsIgnoreCase(
            patchdcnnotexists.andReturn().asString(),
            DCN_NOT_EXISTS
        ));
    }

    @Test
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception {
        String[] dcn = {"987511111111111111111"};
        String[] dcn2 = {"987611111111111111111"};

        //Multiple envelopes with same exception record
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444", dcn,
                                                              "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444", dcn2,
                                                              "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(caseReferenceRequest)
            .contentType(ContentType.JSON)
            .when()
            .put("/bulk-scan-payments/?exception_reference=1111222233334444");

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
            .put("/bulk-scan-payments/?exception_reference=4444333322221111");

        Assert.assertTrue(StringUtils.containsIgnoreCase(
            resultActions.andReturn().asString(),
            EXCEPTION_RECORD_NOT_EXISTS
        ));
    }

    @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception {
        String[] dcn = {"987111111111111111111"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444",
                                                              dcn, "AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .when()
            .patch("/bulk-scan-payments/987111111111111111111/status/PROCESSED");

        //Assert.assertEquals(resultActions.andReturn().getStatusCode(), OK.value());
        Assert.assertNotNull(resultActions.andReturn().asString());
    }

    @Test
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception {

        //Request from Exela with one DCN
        String[] dcn = {"111122224444555511111"};
        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest("111122224444555511111"))
            .when()
            .post("/bulk-scan-payment");

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444",
                                                                                     dcn, "AA08", true);

        //Post request
        Response bsResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");

        //Complete payment
        EnvelopePayment payment = paymentRepository.findByDcnReference("111122224444555511111").get();
        Assert.assertEquals(COMPLETE.toString(), payment.getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findById(payment.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());
        //Assert.assertNotNull(exelaResp.andReturn().asString());
        //Assert.assertNotNull(bsResp.andReturn().asString());
    }

    @Test
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception {

        //Request from Exela with one DCN
        String[] dcn = {"111122223333666611111", "111122223333777711111"};
        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(createPaymentRequest("111122223333666611111"))
            .when()
            .post("/bulk-scan-payment");

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444",
                                                                                     dcn, "AA08", true);

        //Post request
        Response bsResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333666611111").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333777711111").get().getPaymentStatus()
            , INCOMPLETE.toString());
        //Assert.assertNotNull(exelaResp.andReturn().asString());
        //Assert.assertNotNull(bsResp.andReturn().asString());
    }


    @Test
    public void testMatchingBulkScanFirstThenExela() throws Exception {
        //Request from Bulk Scan with one DCN
        String[] dcn = {"111122223333888811111", "111122223333999911111"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444",
                                                                                     dcn, "AA08", true);

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
            .body(createPaymentRequest("111122223333888811111"))
            .when()
            .post("/bulk-scan-payment");

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333888811111").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333999911111").get().getPaymentStatus()
            , INCOMPLETE.toString());
        //Assert.assertNotNull(bsResp.andReturn().asString());
        //Assert.assertNotNull(exelaResp.andReturn().asString());

    }

    @Test
    public void testMatchingMultipleEnvelopesFromExelaBulkScan() throws Exception {
        String dcn1 = "000011112222333311111";
        String dcn2 = "000011112222333411111";

        String dcn[] = {dcn1, dcn2};

        //Request from Exela with DCN dcn1
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(createPaymentRequest(dcn1)).when().post("/bulk-scan-payment");

        //Request from Exela with DCN dcn2
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(createPaymentRequest(dcn2)).when().post("/bulk-scan-payment");

        //Request from bulk scan with Two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);

        Thread.sleep(4000);

        //Post request
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest).when().post("/bulk-scan-payments");

        //Complete payment for DCN dcn1
        EnvelopePayment payment = paymentRepository.findByDcnReference(dcn1).get();
        Assert.assertEquals(COMPLETE.toString(), payment.getPaymentStatus());

        //Envelope complete for DCN dcn2
        Envelope envelopeFirst = envelopeRepository.findById(payment.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), envelopeFirst.getPaymentStatus());

        //Complete payment for DCN dcn2
        EnvelopePayment payment1 = paymentRepository.findByDcnReference(dcn2).get();
        Assert.assertEquals(COMPLETE.toString(), payment1.getPaymentStatus());

        //Envelope complete for DCN dcn2
        Envelope envelopeSecond = envelopeRepository.findById(payment1.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), envelopeSecond.getPaymentStatus());

        //delete envelopes
        envelopeRepository.delete(envelopeFirst);
        envelopeRepository.delete(envelopeSecond);

        //delete payment metadata
        paymentMetadataRepository.delete(paymentMetadataRepository.findByDcnReference(dcn1).get());
        paymentMetadataRepository.delete(paymentMetadataRepository.findByDcnReference(dcn2).get());
    }

    @Test
    public void testProcessNewPaymentsFromExela() throws Exception {

        //Request from Exela with one DCN
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(createPaymentRequest("111122223333123451111")).when().post("/bulk-scan-payment");

        //New payment should be saved with Incomplete status
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333123451111").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }

    @Test
    public void testSearchByCCDForProcessed() throws Exception {
        String dcns[] = {"111166667777888811111", "111166667777999911111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111666677774444"
            , dcns, "AA08", true);

        //Payment Request from Bulk-Scan System
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest).when().post("/bulk-scan-payments");

        //Payment Request from Exela for Payment DCN 1111-6666-7777-8888
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(createPaymentRequest("111166667777888811111")).when().post("/bulk-scan-payment");

        //Payment Request from Exela for Payment DCN 1111-6666-7777-9999
        RestAssured.given().header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .body(createPaymentRequest("111166667777999911111")).when().post("/bulk-scan-payment");

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-8888
        RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .when().patch("/bulk-scan-payments/111166667777888811111/status/PROCESSED");

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-9999
        RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .when().patch("/bulk-scan-payments/111166667777999911111/status/PROCESSED");

        //Calling Search API by DCN and validate response
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("document_control_number", "111166667777999911111");
        Response resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .when().get("/cases", params);

        Assert.assertEquals(200, resultActions.andReturn().getStatusCode());
        Assert.assertEquals(true, resultActions.andReturn().asString().contains("\"all_payments_status\":\"PROCESSED\""));

        //Calling Search API by CCD and validate response
        resultActions = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN).contentType(ContentType.JSON)
            .when().get("/cases/1111666677774444", params);

        Assert.assertEquals(200, resultActions.andReturn().getStatusCode());
        Assert.assertEquals(true, resultActions.andReturn().asString().contains("\"all_payments_status\":\"PROCESSED\""));
    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String[] dcn,
                                                                      String responsibleServiceId, boolean isExceptionRecord) {
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

        String[] dcn = {"111122223333444411111", "111122223333444421111"};
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
        String[] dcn = {"111122223333555511111", "111122223333555521111"};
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

    @Test
    public void testGeneratePaymentReportData_Unprocessed() throws Exception {

        String[] dcn = {"111122223333444411111", "111122223333444421111"};
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
            .get("/report/data");
        Assert.assertEquals(200, response.andReturn().getStatusCode());
    }

    @Test
    public void testGeneratePaymentReportData_DataLoss() throws Exception {
        String[] dcn = {"111122223333555511111", "111122223333555521111"};
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
            .get("/report/data");
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
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(ccd, dcns,
                                                                                     "AA08", true);

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
