package uk.gov.hmcts.reform.bulkscanning.validator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.config.IdamService;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.hmcts.reform.bulkscanning.functionaltest.PaymentControllerFunctionalTest.createBulkScanPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@TestPropertySource(locations="classpath:application-local.yaml")
public class BulkScanValidatorTest {

    @Autowired
    MockMvc mockMvc;

    public static final String RESPONSIBLE_SERVICE_ID_MISSING = "Responsible service id is missing";
    public static final String CCD_REFERENCE_MISSING = "CCD reference is missing";
    public static final String PAYMENT_DCN_MISSING = "Payment DCN are missing";

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED;

    @Before
    public void setUp() {
        if (!TOKENS_INITIALIZED) {
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;
        }
    }

    @Test()
    @Transactional
    public void testFieldLevelValidation() throws Exception{
        String dcn[] = {""};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(null
            ,null,null, false);

        ResultActions resultActions = mockMvc.perform(post("/bulk-scan-payments/")
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .content(asJsonString(bulkScanPaymentRequest))
            .contentType(MediaType.APPLICATION_JSON));

        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));

        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(RESPONSIBLE_SERVICE_ID_MISSING));
        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(resultActions.andReturn().getResponse().getContentAsString().contains(PAYMENT_DCN_MISSING));
    }
}
