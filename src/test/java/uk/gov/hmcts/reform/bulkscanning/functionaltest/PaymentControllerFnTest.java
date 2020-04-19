package uk.gov.hmcts.reform.bulkscanning.functionaltest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.backdoors.RestActions;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;
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

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUID_Roles;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.*;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@AutoConfigureMockMvc(addFilters = false)
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
    PaymentMetadataRepository paymentMetadataRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    ServiceAuthFilter serviceAuthFilter;

    @InjectMocks
    ServiceAndUserAuthFilter serviceAndUserAuthFilter;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    SecurityUtils securityUtils;

    @MockBean
    private JwtDecoder jwtDecoder;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
       //OIDC UserInfo Mocking
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUID_Roles("UID123","payments"));

        caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("9982111111111111")
            .build();

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, objectMapper);

        restActions
            .withAuthorizedService("cmc")
            .withAuthorizedUser()
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"987211111111111111111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233335555"
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
        ResultActions patchRequest = restActions.patch("/bulk-scan-payments/987211111111111111111/status/PROCESSED");

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = restActions.patch("/bulk-scan-payments/987411111111111111111/status/PROCESSED");

        Assert.assertTrue(StringUtils.containsIgnoreCase(patchDCNNotExists.andReturn().getResponse().getContentAsString(),
            DCN_NOT_EXISTS));
    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception {
        String dcn[] = {"987511111111111111111"};
        String dcn2[] = {"987611111111111111111"};

        //Multiple envelopes with same exception record
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn2, "AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = restActions.put("/bulk-scan-payments/?exception_reference=1111222233334444", caseReferenceRequest);

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @Transactional
    @WithMockUser(authorities = "payments")
    public void testExceptionRecordNotExists() throws Exception {

        ResultActions resultActions = restActions.put("/bulk-scan-payments/?exception_reference=4444333322221111", caseReferenceRequest);

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().getResponse().getContentAsString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testMarkPaymentAsProcessed() throws Exception {
        String dcn[] = {"987111111111111111112"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = restActions.patch("/bulk-scan-payments/987111111111111111112/status/PROCESSED");

        Assert.assertEquals(resultActions.andReturn().getResponse().getStatus(), OK.value());

        EnvelopePayment payment1 = paymentRepository.findByDcnReference("987111111111111111112").get();
        Assert.assertEquals(PROCESSED.toString(), payment1.getPaymentStatus());

        //Delete Envelope for DCN 1111-2222-3333-4444
        Envelope envelopeSecond = envelopeRepository.findById(payment1.getEnvelope().getId()).get();

        //delete envelope
        envelopeRepository.delete(envelopeSecond);
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"111122224444555511111"};
        restActions.post("/bulk-scan-payment", createPaymentRequest("111122224444555511111"));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Complete payment
        EnvelopePayment payment = paymentRepository.findByDcnReference("111122224444555511111").get();
        Assert.assertEquals(COMPLETE.toString(), payment.getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findById(payment.getEnvelope().getId()).get();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());
    }


    @Test
    @WithMockUser(authorities = "payments")
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception {

        //Request from Exela with one DCN
        String dcn[] = {"111122223333666611111", "111122223333777711111"};
        restActions.post("/bulk-scan-payment", createPaymentRequest("111122223333666611111"));

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333666611111").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333777711111").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }


    @Test
    @WithMockUser(authorities = "payments")
    public void testMatchingBulkScanFirstThenExela() throws Exception {
        //Request from Bulk Scan with one DCN
        String dcn[] = {"111122223333888811111", "111122223333999911111"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        restActions.post("/bulk-scan-payment", createPaymentRequest("111122223333888811111"));

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333888811111").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333999911111").get().getPaymentStatus()
            , INCOMPLETE.toString());

    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testMatchingMultipleEnvelopesFromExelaBulkScan() throws Exception {
        String dcn1 = "000011112222333311111";
        String dcn2 = "000011112222333411111";

        String dcn[] = {dcn1, dcn2};

        //Request from Exela with DCN dcn1
        restActions.post("/bulk-scan-payment", createPaymentRequest(dcn1));

        //Request from Exela with DCN dcn2
        restActions.post("/bulk-scan-payment", createPaymentRequest(dcn2));

        //Request from bulk scan with Two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111222233334444"
            , dcn, "AA08", true);

        Thread.sleep(4000);

        //Post request
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

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
    @WithMockUser(authorities = "payments")
    public void testProcessNewPaymentsFromExela() throws Exception {

        //Request from Exela with one DCN
        restActions.post("/bulk-scan-payment",
                         createPaymentRequest("111122223333123451111"));

        //New payment should be saved with Incomplete status
        Assert.assertEquals(paymentRepository.findByDcnReference("111122223333123451111").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testSearchByCCDForProcessed() throws Exception {
        String dcns[] = {"111166667777888811111", "111166667777999911111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111666677774444"
            , dcns, "AA08", true);

        //Payment Request from Bulk-Scan System
        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        //Payment Request from Exela for Payment DCN 1111-6666-7777-8888
        restActions.post("/bulk-scan-payment", createPaymentRequest("111166667777888811111"));

        //Payment Request from Exela for Payment DCN 1111-6666-7777-9999
        restActions.post("/bulk-scan-payment", createPaymentRequest("111166667777999911111"));

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-8888
        restActions.patch("/bulk-scan-payments/111166667777888811111/status/PROCESSED");

        //Update Payment Status once Payment Allocated to Fee for DCN 1111-6666-7777-9999
        restActions.patch("/bulk-scan-payments/111166667777999911111/status/PROCESSED");

        //Calling Search API by DCN and validate response
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("document_control_number", "111166667777999911111");
        ResultActions resultActions = restActions.get("/cases", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
        Assert.assertEquals(true, resultActions.andReturn().getResponse().getContentAsString().contains("\"all_payments_status\":\"PROCESSED\""));

        //Calling Search API by CCD and validate response
        resultActions = restActions.get("/cases/1111666677774444", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
        Assert.assertEquals(true, resultActions.andReturn().getResponse().getContentAsString().contains("\"all_payments_status\":\"PROCESSED\""));
    }

    @Test
    @WithMockUser(authorities = "unauthorizedPaymentsRole")
    public void testUnAuthorisedUserAccessDeniedHandlerTest() throws Exception {

        //Calling Search API by DCN and validate response
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("document_control_number", "DCN12341234123412");
        ResultActions resultActions = restActions.get("/cases", params);

        Assert.assertEquals(403, resultActions.andReturn().getResponse().getStatus());
    }


    @Test
    @WithMockUser(authorities = "payments")
    public void testInvalidAuthorisedService() throws Exception {
        String dcns[] = {"111166667777888821111", "111166667777999921111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111666677775555"
            , dcns, "AA08", true);

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        RestActions testRestAction = new RestActions(mvc, objectMapper);
        testRestAction
            .withAuthorizedService("test-invalid")
            .withReturnUrl("https://www.gooooogle.com");
        //Payment Request from Bulk-Scan System
        ResultActions resultActions = testRestAction.post("/bulk-scan-payments", bulkScanPaymentRequest);

        Assert.assertEquals(403, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "UnAuthorisedPaymentRole")
    public void testInvalidAuthorisedUser() throws Exception {
        String dcns[] = {"111166667777888821111", "111166667777999921111"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111666677775555"
            , dcns, "AA08", true);

        restActions.post("/bulk-scan-payments", bulkScanPaymentRequest);

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        RestActions testRestAction = new RestActions(mvc, objectMapper);
        testRestAction
            .withAuthorizedService("")
            .withReturnUrl("https://www.gooooogle.com");

        //Calling Search API by DCN and validate response
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("document_control_number", "111166667777999921111");
        ResultActions resultActions = testRestAction.get("/cases", params);

        Assert.assertEquals(403, resultActions.andReturn().getResponse().getStatus());
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
    @WithMockUser(authorities = "payments")
    public void testGeneratePaymentReport_Unprocessed() throws Exception {

        String dcn[] = {"111122223333444411111", "111122223333444421111"};
        String ccd = "1111222233334444";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = restActions.get("/report/download", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testGeneratePaymentReport_DataLoss() throws Exception {
        String dcn[] = {"111122223333555511111", "111122223333555521111"};
        createTestReportDataLoss(dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        ResultActions resultActions = restActions.get("/report/download", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testGetPaymentReportData_DataLoss() throws Exception {
        String dcn[] = {"111122223333555511111", "111122223333555521111"};
        createTestReportDataLoss(dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        ResultActions resultActions = restActions.get("/report/data", params);

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testGetPaymentReportData_Unprocessed() throws Exception {
        String dcn[] = {"111122223333555511111", "111122223333555521111"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 365 * 24 * 60 * 60 * 1000L)));
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

    private void createTestReportDataLoss(String... dcns) throws Exception {
        //Request from Exela with one DCN

        restActions.post("/bulk-scan-payment", createPaymentRequest(dcns[0]));
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }
}
