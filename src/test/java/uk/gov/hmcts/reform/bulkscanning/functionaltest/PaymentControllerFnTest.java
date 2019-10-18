package uk.gov.hmcts.reform.bulkscanning.functionaltest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.backdoors.RestActions;
import uk.gov.hmcts.reform.bulkscanning.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.reform.bulkscanning.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
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

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"local", "test"})
//@TestPropertySource(locations="classpath:application-local.yaml")
public class PaymentControllerFnTest {

    MockMvc mvc;

    @Autowired
    PaymentService bulkScanConsumerService;

    BulkScanPaymentRequest bulkScanPaymentRequest;

    CaseReferenceRequest caseReferenceRequest;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("cmc")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"DCN2"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-5555"
            ,dcn,"AA08", true);

        //Post request
        ResultActions resultActions = restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

        //Post Repeat request
        ResultActions repeatRequest = restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        Assert.assertTrue(StringUtils.containsIgnoreCase(
            repeatRequest.andReturn().getResponse().getContentAsString(),
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST
        ));

        //PATCH Request
        ResultActions patchRequest = restActions.patch("/bulk-scan-payments/DCN2/status/PROCESSED");

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = restActions.patch("/bulk-scan-payments/DCN4/status/PROCESSED");

        Assert.assertTrue(StringUtils.containsIgnoreCase(patchDCNNotExists.andReturn().getResponse().getContentAsString(),
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

        ResultActions resultActions = restActions.put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444", caseReferenceRequest);

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @Transactional
    public void testExceptionRecordNotExists() throws Exception {

        ResultActions resultActions = restActions.put("/bulk-scan-payments/?exception_reference=4444-3333-2222-111", caseReferenceRequest);

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().getResponse().getContentAsString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }

    @Test
    public void testMarkPaymentAsProcessed() throws Exception {
        String dcn[] = {"DCN1"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = restActions.patch("/bulk-scan-payments/DCN1/status/PROCESSED");

        Assert.assertEquals(resultActions.andReturn().getResponse().getStatus(), OK.value());
    }

    @Test
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-4444-5555"};
        restActions.post("/bulk-scan-payment", createPaymentRequest("1111-2222-4444-5555"));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Complete payment
        EnvelopePayment payment = paymentRepository.findByDcnReference("1111-2222-4444-5555").get();
        Assert.assertEquals(COMPLETE.toString(), payment.getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findById(payment.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());
    }


    @Test
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-3333-6666", "1111-2222-3333-7777"};
        restActions.post("/bulk-scan-payment", createPaymentRequest("1111-2222-3333-6666"));

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-6666").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-7777").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }


    @Test
    public void testMatchingBulkScanFirstThenExela() throws Exception {
        //Request from Bulk Scan with one DCN
        String dcn[] = {"1111-2222-3333-8888", "1111-2222-3333-9999"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        restActions.post("/bulk-scan-payment", createPaymentRequest("1111-2222-3333-8888"));

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-8888").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-9999").get().getPaymentStatus()
            , INCOMPLETE.toString());

    }

    @Test
    public void testProcessNewPaymentsFromExela() throws Exception {

        //Request from Exela with one DCN
        restActions.post("/bulk-scan-payment",
                         createPaymentRequest("1111-2222-3333-12345"));

        //New payment should be saved with Incomplete status
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-12345").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }

    @Test
    public void testSearchByCCDForProcessed() throws Exception {
        String dcns[] = {"1111-6666-7777-8888", "1111-6666-7777-9999"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-6666-7777-4444"
            , dcns, "AA08", true);

        //Payment Request from Bulk-Scan System
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Payment Request from Exela for Payment DCN 1111-6666-7777-8888
        restActions.post("/bulk-scan-payment", createPaymentRequest("1111-6666-7777-8888"));

        //Payment Request from Exela for Payment DCN 1111-6666-7777-9999
        restActions.post("/bulk-scan-payment", createPaymentRequest("1111-6666-7777-9999"));

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-8888
        restActions.patch("/bulk-scan-payments/1111-6666-7777-8888/status/PROCESSED");

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-9999
        restActions.patch("/bulk-scan-payments/1111-6666-7777-9999/status/PROCESSED");

        //Calling Search API by DCN and validate response
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("document_control_number", "1111-6666-7777-9999");
        ResultActions resultActions = restActions.get("/cases", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
        Assert.assertEquals(true, resultActions.andReturn().getResponse().getContentAsString().contains("\"all_payments_status\":\"PROCESSED\""));

        //Calling Search API by CCD and validate response
        resultActions = restActions.get("/cases/1111-6666-7777-4444", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
        Assert.assertEquals(true, resultActions.andReturn().getResponse().getContentAsString().contains("\"all_payments_status\":\"PROCESSED\""));
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
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = restActions.get("/report/download", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGeneratePaymentReport_DataLoss() throws Exception {
        String dcn[] = {"11112222333355551", "11112222333355552"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        ResultActions resultActions = restActions.get("/report/download", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReportData_DataLoss() throws Exception {
        String dcn[] = {"11112222333355551", "11112222333355552"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        ResultActions resultActions = restActions.get("/report/data", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReportData_Unprocessed() throws Exception {
        String dcn[] = {"11112222333355551", "11112222333355552"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = restActions.get("/report/data", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    private void createTestReportData(String ccd, String... dcns) throws Exception {
        //Request from Exela with one DCN

        restActions.post("/bulk-scan-payment", createPaymentRequest(dcns[0]));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(ccd
            , dcns, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
