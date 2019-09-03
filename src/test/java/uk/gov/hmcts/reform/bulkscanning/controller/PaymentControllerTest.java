package uk.gov.hmcts.reform.bulkscanning.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.ExelaPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations="classpath:application-local.yaml")
public class PaymentControllerTest {

    MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    /*@Test
    public void testBulkScanningPaymentRequestFirst() throws Exception{
        String dcn[] = {"DCN2"};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-5555"
            ,dcn,"AA08");

        //Post request
        ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

        //Post Repeat request
        ResultActions repeatRequest = mockMvc.perform(post("/bulk-scan-payments")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(repeatRequest.andReturn().getResponse().getContentAsString(),
            BULK_SCANNING_PAYMENT_DETAILS_ALREADY_EXIST));

        //PATCH Request
        ResultActions patchRequest = mockMvc.perform(patch("/bulk-scan-payments/DCN2/process")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(patchRequest.andReturn().getResponse().getContentAsString());

        //DCN Not exists Request
        ResultActions patchDCNNotExists = mockMvc.perform(patch("/bulk-scan-payments/DCN3/process")
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
        String dcn[] = {"DCN1"};
        paymentService.saveInitialMetadataFromBs(createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08"));

        ResultActions resultActions = mockMvc.perform(put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(createCaseReferenceRequest()))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }

    @Test
    @Transactional
    public void testExceptionRecordNotExists() throws Exception{

        ResultActions resultActions = mockMvc.perform(put("/bulk-scan-payments/?exception_reference=4444-3333-2222-111")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(createCaseReferenceRequest()))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertTrue(StringUtils.containsIgnoreCase(resultActions.andReturn().getResponse().getContentAsString(),
            EXCEPTION_RECORD_NOT_EXISTS));
    }*/

   /* @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception{
        String dcn[] = {"DCN1"};
        bulkScanPaymentRequest = createBulkScanPaymentRequest("1111-2222-3333-4444"
            ,dcn,"AA08");
        bulkScanConsumerService.saveInitialMetadataFromBs(bulkScanPaymentRequest);

        ResultActions resultActions = mvc.perform(patch("/bulk-scan-payments/DCN1/PROCESS")
            .header("ServiceAuthorization", "service")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        Assert.assertNotNull(resultActions.andReturn().getResponse().getContentAsString());

    }*/

    public BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String dcn[], String responsibleServiceId) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(responsibleServiceId)
            .isExceptionRecord(true)
            .build();
    }

    @Test
    @Transactional
    public void testCreatePaymentFromExela() throws Exception{

        ResultActions resultActions = mockMvc.perform(put("/bulk-scan-payments/111222333")
                                                      .header("ServiceAuthorization", "service")
                                                      .content(asJsonString(createPaymentRequest()))
                                                      .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    @Transactional
    public void testSearchPaymentWithCCD() throws Exception{
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("CCD123")
            .build();
        when(paymentService.retrieveByCCDReference(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases/CCD123")
                                                      .header("ServiceAuthorization", "service")
                                                        .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    @Transactional
    public void testSearchPaymentWithDcn() throws Exception{
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("CCD123")
            .build();
        when(paymentService.retrieveByDcn(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases")
            .param("document_control_number", "DCN123")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    private ExelaPaymentRequest createPaymentRequest() {
        return ExelaPaymentRequest.createPaymentRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .bankedDate(new Date())
            .bankGiroCreditSlipNumber("BGC123")
            .currency("GBP")
            .method("CHEQUE")
            .build();
    }

    /*private CaseReferenceRequest createCaseReferenceRequest(){
        return CaseReferenceRequest
            .createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();
    }*/

}
