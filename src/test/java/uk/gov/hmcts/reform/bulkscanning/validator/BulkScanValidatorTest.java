package uk.gov.hmcts.reform.bulkscanning.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.backdoors.RestActions;
import uk.gov.hmcts.reform.bulkscanning.config.security.filiters.ServiceAndUserAuthFilter;
import uk.gov.hmcts.reform.bulkscanning.config.security.utils.SecurityUtils;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.functionaltest.PaymentControllerFnTest.createBulkScanPaymentRequest;

@RunWith(SpringRunner.class)
@SpringBootTest
@EnableFeignClients
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
//@TestPropertySource(locations="classpath:application-local.yaml")
public class BulkScanValidatorTest {

    MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    ServiceAuthFilter serviceAuthFilter;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    public static final String RESPONSIBLE_SERVICE_ID_MISSING = "site_id can't be Blank";
    public static final String CCD_REFERENCE_MISSING = "ccd_case_number can't be Blank";
    public static final String PAYMENT_DCN_MISSING = "document_control_numbers can't be Blank";
    public static final String UNKNOWN_FIELD = "Unknown field";

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, objectMapper);

        restActions
            .withAuthorizedService("cmc")
            .withReturnUrl("https://www.gooooogle.com");
    }

    @Test()
    @Transactional
    public void testFieldLevelValidation() throws Exception{
        String dcn[] = {""};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(null
            , null, "AA08", false);

        ResultActions resultActions = restActions.post("/bulk-scan-payments/", bulkScanPaymentRequest);

        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));

        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(PAYMENT_DCN_MISSING));
    }

    @Test()
    @Transactional
    public void testRequestValidation_AdditionalFields() throws Exception{


        ResultActions resultActions = restActions.post("/bulk-scan-payment/", new ExelaPayment(BigDecimal.ONE,
                                                                                                "123456",
                                                                                                "2019-01-01",
                                                                                                "GBP",
                                                                                                "111122223333444481111",
                                                                                                "CASH",
                                                                                                "Unknown"));

        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));

        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(UNKNOWN_FIELD));
    }

    @AllArgsConstructor
    @Getter
    class ExelaPayment {
        private BigDecimal amount;
        private String bank_giro_credit_slip_number;
        private String banked_date;
        private String currency;
        private String document_control_number;
        private String method;
        private String additional_field;
    }
}
