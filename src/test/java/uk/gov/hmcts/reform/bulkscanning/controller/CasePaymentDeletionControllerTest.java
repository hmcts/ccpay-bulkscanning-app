package uk.gov.hmcts.reform.bulkscanning.controller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import uk.gov.hmcts.reform.bulkscanning.audit.AppInsightsAuditRepository;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentDeletionException;
import uk.gov.hmcts.reform.bulkscanning.service.CasePaymentDeletionService;
import uk.gov.hmcts.reform.bulkscanning.validator.BulkScanControllerValidator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class CasePaymentDeletionControllerTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";

    private MockMvc mockMvc;

    @Mock
    private CasePaymentDeletionService casePaymentDeletionService;

    @Mock
    private AppInsightsAuditRepository auditRepository;

    @Before
    public void setUp() {
        CasePaymentDeletionController controller = new CasePaymentDeletionController(casePaymentDeletionService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        ProxyFactory proxyFactory = new ProxyFactory(controller);
        proxyFactory.addAdvice(new MethodValidationInterceptor((jakarta.validation.Validator) validator));

        mockMvc = MockMvcBuilders.standaloneSetup(proxyFactory.getProxy())
            .setControllerAdvice(new BulkScanControllerValidator(auditRepository))
            .build();
    }

    @Test
    public void testDeleteByCcdCaseReference_success() throws Exception {
        doNothing().when(casePaymentDeletionService).deleteByCcdCaseReference(CCD_CASE_NUMBER);

        mockMvc.perform(delete(deletionPath(CCD_CASE_NUMBER))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNoContent());

        verify(casePaymentDeletionService, times(1)).deleteByCcdCaseReference(CCD_CASE_NUMBER);
    }

    @Test
    public void testDeleteByCcdCaseReference_invalidLength_returnsBadRequest() throws Exception {
        mockMvc.perform(delete(deletionPath("12345"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(casePaymentDeletionService, never()).deleteByCcdCaseReference(anyString());
    }

    @Test
    public void testDeleteByCcdCaseReference_nonNumeric_returnsBadRequest() throws Exception {
        mockMvc.perform(delete(deletionPath("174773847038316A"))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        verify(casePaymentDeletionService, never()).deleteByCcdCaseReference(anyString());
    }

    @Test
    public void testDeleteByCcdCaseReference_unexpectedError_returnsInternalServerError() throws Exception {
        doThrow(new PaymentDeletionException("Unexpected error occurred during deletion"))
            .when(casePaymentDeletionService).deleteByCcdCaseReference(CCD_CASE_NUMBER);

        mockMvc.perform(delete(deletionPath(CCD_CASE_NUMBER))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Unexpected error occurred during deletion")));

        verify(casePaymentDeletionService, times(1)).deleteByCcdCaseReference(CCD_CASE_NUMBER);
    }

    private static String deletionPath(String ccdCaseNumber) {
        return "/cases/" + ccdCaseNumber + "/bulk-scan-payments";
    }
}
