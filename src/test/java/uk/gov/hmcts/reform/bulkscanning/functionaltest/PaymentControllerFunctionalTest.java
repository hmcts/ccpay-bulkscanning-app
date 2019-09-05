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
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
            ,dcn,"AA08");

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
        ResultActions patchRequest = mvc.perform(patch("/bulk-scan-payments/DCN2/process")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = mvc.perform(patch("/bulk-scan-payments/DCN3/process")
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
            ,dcn,"AA08");
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn2,"AA08");
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(put("/bulk-scan-cases/?exception_reference=1111-2222-3333-4444")
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

        ResultActions resultActions = mvc.perform(put("/bulk-scan-cases/?exception_reference=4444-3333-2222-111")
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
            ,dcn,"AA08");
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(patch("/bulk-scan-payments/DCN1/process")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertEquals(resultActions.andReturn().getResponse().getStatus(), OK.value());

    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String dcn[], String responsibleServiceId) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(true)
            .build();
    }


}
