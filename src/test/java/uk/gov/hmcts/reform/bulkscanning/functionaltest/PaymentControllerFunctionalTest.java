package uk.gov.hmcts.reform.bulkscanning.functionaltest;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningConstants.*;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application-local.yaml")
public class PaymentControllerFunctionalTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    PaymentService bulkScanConsumerService;

    BulkScanPaymentRequest bulkScanPaymentRequest;

    CaseReferenceRequest caseReferenceRequest;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Before
    @Transactional
    public void setUp() {
        caseReferenceRequest = CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();
    }

    @Test
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"DCN2"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-5555"
            ,dcn,"AA08", true);

        //Post request
        ResultActions resultActions = mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

        //Post Repeat request
        ResultActions repeatRequest = mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(repeatRequest.andReturn().getResponse().getContentAsString(),
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST));

        //PATCH Request
        ResultActions patchRequest = mvc.perform(patch("/bulk-scan-payments/DCN2/status/PROCESSED")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = mvc.perform(patch("/bulk-scan-payments/DCN3/status/PROCESSED")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(patchDCNNotExists.andReturn().getResponse().getContentAsString(),
            DCN_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception{
        String dcn[] = {"DCN5"};
        String dcn2[] = {"DCN6"};

        //Multiple envelopes with same exception record
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn2,"AA08", true);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(caseReferenceRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @Transactional
    public void testExceptionRecordNotExists() throws Exception{

        ResultActions resultActions = mvc.perform(put("/bulk-scan-payments/?exception_reference=4444-3333-2222-111")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(caseReferenceRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().getResponse().getContentAsString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }

    @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception{
        String dcn[] = {"DCN1"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", false);
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(patch("/bulk-scan-payments/DCN1/status/PROCESSED")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertEquals(resultActions.andReturn().getResponse().getStatus(), OK.value());

    }

    @Test
    public void testMatchingPaymentsFromExcelaBulkScan() throws Exception{

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-4444-5555"};
        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(createPaymentRequest("1111-2222-4444-5555")))
            .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //Complete payment
        Assert.assertEquals(COMPLETE.toString(), paymentRepository.findByDcnReference("1111-2222-4444-5555").get().getPaymentStatus());

        //Complete envelope
        Envelope finalEnvelope = envelopeRepository.findAll().iterator().next();
        Assert.assertEquals(COMPLETE.toString(), finalEnvelope.getPaymentStatus());
    }


    @Test
    public void testNonMatchingPaymentsFromExelaThenBulkScan() throws Exception{

        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-3333-6666","1111-2222-3333-7777"};
        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(createPaymentRequest("1111-2222-3333-6666")))
            .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-6666").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-7777").get().getPaymentStatus()
            , INCOMPLETE.toString());
    }



    @Test
    public void testMatchingBulkScanFirstThenExela() throws Exception{
        //Request from Bulk Scan with one DCN
        String dcn[] = {"1111-2222-3333-8888","1111-2222-3333-9999"};

        //Request from bulk scan with two DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));


        mvc.perform(post("/bulk-scan-payment")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(createPaymentRequest("1111-2222-3333-8888")))
            .contentType(MediaType.APPLICATION_JSON));



        //Complete payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-8888").get().getPaymentStatus()
            , COMPLETE.toString());

        //Non Complete Payment
        Assert.assertEquals(paymentRepository.findByDcnReference("1111-2222-3333-9999").get().getPaymentStatus()
            , INCOMPLETE.toString());

    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String[] dcn, String responsibleServiceId, boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    @Test
    public void testGeneratePaymentReport_Unprocessed() throws Exception{
        createTestReportData();
        mvc.perform(get("/report/download")
                        .header("ServiceAuthorization", "service")
                        .param("date_from", "01/01/2011")
                        .param("date_to", "01/10/2011")
                        .param("report_type", "UNPROCESSED")
                        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    }

    @Test
    public void testGeneratePaymentReport_DataLoss() throws Exception{
        createTestReportData();
        mvc.perform(get("/report/download")
                        .header("ServiceAuthorization", "service")
                        .param("date_from", "01/01/2011")
                        .param("date_to", "01/10/2011")
                        .param("report_type", "DATA_LOSS")
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    public void createTestReportData() throws Exception{
        //Request from Exela with one DCN
        String dcn[] = {"1111-2222-4444-5555", "1111-2222-4444-6666"};
        mvc.perform(post("/bulk-scan-payment")
                        .header("ServiceAuthorization", "service")
                        .content(asJsonString(createPaymentRequest("1111-2222-4444-5555")))
                        .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08", true);

        //Post request
        mvc.perform(post("/bulk-scan-payments")
                        .header("ServiceAuthorization", "service")
                        .content(asJsonString(bulkScanPaymentRequest))
                        .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
