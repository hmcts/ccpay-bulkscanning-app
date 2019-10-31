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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.service.PaymentServiceTest.CCD_CASE_REFERENCE;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
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
    @Transactional
    public void testCreatePaymentFromExela() throws Exception{

        ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payment/")
                                                      .header("ServiceAuthorization", "service")
                                                      .content(asJsonString(createPaymentRequest("111222333")))
                                                      .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(201), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testCreatePaymentFromExela_Conflict() throws Exception{

        Optional<PaymentMetadata> paymentMetadata = Optional.of(PaymentMetadata.paymentMetadataWith()
                                                                    .id(1).amount(BigDecimal.valueOf(100))
                                                                    .dcnReference("111222333")
                                                                    .dateBanked(LocalDateTime.now())
                                                                    .paymentMethod(CHEQUE.toString()).currency(GBP.toString())
                                                                    .build());

        when(paymentService.getPaymentMetadata(any(String.class))).thenReturn(paymentMetadata.get());
        ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payment/")
                                                          .header("ServiceAuthorization", "service")
                                                          .content(asJsonString(createPaymentRequest("111222333")))
                                                          .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(409), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    @Transactional
    public void testCreatePaymentFromExela_withException() throws Exception{

        when(paymentService.getPaymentMetadata(any(String.class)))
            .thenThrow(new PaymentException("Exception in fetching Metadata"));
        ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payment/")
                                                          .header("ServiceAuthorization", "service")
                                                          .content(asJsonString(createPaymentRequest("111222333")))
                                                          .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(true, resultActions.andReturn().getResponse()
            .getContentAsString().contains("Exception in fetching Metadata"));
    }*/

    //Test cases for Bulk Scan endpoints bulk scan
   @Test
   @Transactional
   public void testCreatePaymentForBulkScan() throws Exception{
       String dcn[] = {"DCN1","DCN2"};
       BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(CCD_CASE_REFERENCE
           ,dcn,"AA08");

       when(paymentService.saveInitialMetadataFromBs(any(BulkScanPaymentRequest.class)))
           .thenReturn(Arrays.asList(dcn));

       ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payments/")
           .header("ServiceAuthorization", "service")
           .content(asJsonString(bulkScanPaymentRequest))
           .contentType(MediaType.APPLICATION_JSON));

       Assert.assertEquals(Integer.valueOf(201), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
   }

    @Test
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception{
        CaseReferenceRequest caseReferenceRequest = CaseReferenceRequest.createCaseReferenceRequest()
            .ccdCaseNumber("CCN2")
            .build();

        ResultActions resultActions = mockMvc.perform(put("/bulk-scan-payments/?exception_reference=1111-2222-3333-4444")
            .header("ServiceAuthorization", "service")
            .content(asJsonString(caseReferenceRequest))
            .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception{
        ResultActions resultActions = mockMvc.perform(patch("/bulk-scan-payments/DCN2/status/PROCESSED")
          .header("Authorization", "user")
          .header("ServiceAuthorization", "service")
          .contentType(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    public static Envelope mockBulkScanningEnvelope() {
        Envelope bsEnvelope =  Envelope.envelopeWith().id(1).dateUpdated(LocalDateTime.now()).dateCreated(LocalDateTime.now()).build();

        EnvelopePayment payment1 = EnvelopePayment.paymentWith().id(1).dcnReference("dcn1").envelope(bsEnvelope)
            .dateUpdated(LocalDateTime.now()).dateCreated(LocalDateTime.now()).build();
        EnvelopePayment payment2 = EnvelopePayment.paymentWith().id(2).dcnReference("dcn2").envelope(bsEnvelope)
            .dateUpdated(LocalDateTime.now()).dateCreated(LocalDateTime.now()).build();

        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();
        envelopePaymentList.add(payment1);
        envelopePaymentList.add(payment2);

        EnvelopeCase envelopeCase = EnvelopeCase.caseWith().id(1).ccdReference(CCD_CASE_REFERENCE).envelope(bsEnvelope)
            .dateUpdated(LocalDateTime.now()).dateCreated(LocalDateTime.now()).build();

        List<EnvelopeCase> envelopeCasesList = new ArrayList<>();
        envelopeCasesList.add(envelopeCase);

         bsEnvelope =  Envelope
            .envelopeWith()
            .id(1)
            .envelopePayments(envelopePaymentList)
            .envelopeCases(envelopeCasesList)
             .dateUpdated(LocalDateTime.now())
             .dateCreated(LocalDateTime.now())
             .build();
        return bsEnvelope;
    }

    public static BulkScanPayment createPaymentRequest(String dcnReference) {
        return BulkScanPayment.createPaymentRequestWith()
            .dcnReference(dcnReference)
            .amount(BigDecimal.valueOf(100.00))
            .bankedDate(LocalDateTime.now())
            .bankGiroCreditSlipNumber(Integer.parseInt("BGC123"))
            .currency(Currency.valueOf("GBP").toString())
            .method(PaymentMethod.valueOf("CHEQUE").toString())
            .build();
    }

    public BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String dcn[], String responsibleServiceId) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(ResponsibleSiteId.valueOf(responsibleServiceId).toString())
            .isExceptionRecord(true)
            .build();
    }
}
