package uk.gov.hmcts.reform.bulkscanning.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.backdoors.RestActions;
import uk.gov.hmcts.reform.bulkscanning.backdoors.ServiceResolverBackdoor;
import uk.gov.hmcts.reform.bulkscanning.backdoors.UserResolverBackdoor;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

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
    protected ServiceResolverBackdoor serviceRequestAuthorizer;

    @Autowired
    protected UserResolverBackdoor userRequestAuthorizer;

    private static final String USER_ID = UserResolverBackdoor.AUTHENTICATED_USER_ID;

    RestActions restActions;

    @Autowired
    private ObjectMapper objectMapper;

    public static final String RESPONSIBLE_SERVICE_ID_MISSING = "Responsible service id is missing";
    public static final String CCD_REFERENCE_MISSING = "CCD reference is missing";
    public static final String PAYMENT_DCN_MISSING = "Payment DCN are missing";

    @Before
    public void setUp() {
        MockMvc mvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        this.restActions = new RestActions(mvc, serviceRequestAuthorizer, userRequestAuthorizer, objectMapper);

        restActions
            .withAuthorizedService("cmc")
            .withAuthorizedUser(USER_ID)
            .withUserId(USER_ID)
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
}
