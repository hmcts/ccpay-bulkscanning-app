package uk.gov.hmcts.reform.bulkscanning.functionaltest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.backdoors.RestActions;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilterTest.getUserInfoBasedOnUidRoles;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.functionaltest.PaymentControllerFnTest.createBulkScanPaymentRequest;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@AutoConfigureMockMvc
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles({"local", "test"})
public class CasePaymentDeletionControllerFnTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";
    private static final String DCN = "174773847038316811111";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EnvelopeCaseRepository envelopeCaseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentMetadataRepository paymentMetadataRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SecurityUtils securityUtils;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private RestActions restActions;

    @Before
    public void setUp() {
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUidRoles("UID123", "disposer-payment-user"));

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, objectMapper);
        restActions
            .withAuthorizedService("disposer-fee-and-pay")
            .withAuthorizedUser()
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test
    @WithMockUser(authorities = "disposer-payment-user")
    public void testDeleteByCcdCaseReference_success() {
        seedBulkScanPaymentData(CCD_CASE_NUMBER, DCN);

        Optional<List<EnvelopeCase>> seededCases = envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER);
        Assert.assertTrue(seededCases.isPresent() && !seededCases.get().isEmpty());
        Assert.assertTrue(paymentRepository.findByDcnReference(DCN).isPresent());
        Assert.assertTrue(paymentMetadataRepository.findByDcnReference(DCN).isPresent());

        ResultActions resultActions = restActions.delete("/cases/" + CCD_CASE_NUMBER + "/bulk-scan-payments");

        Assert.assertEquals(204, resultActions.andReturn().getResponse().getStatus());

        Optional<List<EnvelopeCase>> envelopeCases = envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER);
        Assert.assertTrue(envelopeCases.isEmpty() || envelopeCases.get().isEmpty());

        Optional<EnvelopePayment> payment = paymentRepository.findByDcnReference(DCN);
        Assert.assertTrue(payment.isEmpty());

        Optional<PaymentMetadata> metadata = paymentMetadataRepository.findByDcnReference(DCN);
        Assert.assertTrue(metadata.isEmpty());
    }

    @Test
    @WithMockUser(authorities = "disposer-payment-user")
    public void testDeleteByCcdCaseReference_noContent_isIdempotent() {
        ResultActions resultActions = restActions.delete("/cases/1999888877776666/bulk-scan-payments");

        Assert.assertEquals(204, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "disposer-payment-user")
    public void testDeleteByCcdCaseReference_requiresAuthorizedService() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        RestActions unauthenticatedRestActions = new RestActions(mvc, objectMapper);

        ResultActions resultActions = unauthenticatedRestActions.delete("/cases/" + CCD_CASE_NUMBER + "/bulk-scan-payments");

        Assert.assertEquals(401, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "payments")
    public void testDeleteByCcdCaseReference_requiresDisposerRole() {
        when(securityUtils.getUserInfo()).thenReturn(getUserInfoBasedOnUidRoles("UID123", "payments"));

        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        RestActions unauthorizedRoleRestActions = new RestActions(mvc, objectMapper);
        unauthorizedRoleRestActions
            .withAuthorizedService("disposer-fee-and-pay")
            .withAuthorizedUser()
            .withReturnUrl("https://www.gooooogle.com");

        ResultActions resultActions = unauthorizedRoleRestActions.delete("/cases/" + CCD_CASE_NUMBER + "/bulk-scan-payments");

        Assert.assertEquals(403, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    @WithMockUser(authorities = "disposer-payment-user")
    public void testDeleteByCcdCaseReference_invalidService_returnsForbidden() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        RestActions invalidServiceRestActions = new RestActions(mvc, objectMapper);
        invalidServiceRestActions
            .withAuthorizedService("test-invalid")
            .withAuthorizedUser()
            .withReturnUrl("https://www.gooooogle.com");

        ResultActions resultActions = invalidServiceRestActions.delete("/cases/" + CCD_CASE_NUMBER + "/bulk-scan-payments");

        Assert.assertEquals(403, resultActions.andReturn().getResponse().getStatus());
    }

    private void seedBulkScanPaymentData(String ccdCaseNumber, String dcn) {
        String[] dcns = {dcn};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(
            ccdCaseNumber, dcns, "AA08", false);
        paymentService.saveInitialMetadataFromBs(bulkScanPaymentRequest);
        paymentService.processPaymentFromExela(createPaymentRequest(dcn), dcn);
    }
}
