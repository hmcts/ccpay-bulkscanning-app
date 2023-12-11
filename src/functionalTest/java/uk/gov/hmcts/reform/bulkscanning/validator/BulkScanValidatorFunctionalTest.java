package uk.gov.hmcts.reform.bulkscanning.validator;

import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanning.config.BulkScanPaymentTestService;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerFunctionalTest.createBulkScanCcdPayments;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@EnableFeignClients
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class BulkScanValidatorFunctionalTest {

    public static final String RESPONSIBLE_SERVICE_ID_MISSING = "site_id can't be Blank";
    public static final String CCD_REFERENCE_MISSING = "ccd_case_number can't be Blank";
    public static final String PAYMENT_DCN_MISSING = "document_control_numbers can't be Blank";
    public static final String INVALID_SITE_ID = "Invalid site_id. Accepted values are AA08 or AA07 or AA09 or ABA1";
    public static final String INVALID_SITE_ID_LENGTH = "site_id length must be 4 Characters";
    public static final String INVALID_CCD_REFERENCE_LENGTH = "ccd_case_number length must be 16 digits";
    public static final String INVALID_DCN_LENGTH = "document_control_number must be 21 digit numeric";
    public static final String INVALID_CCD_REFERENCE_TYPE = "ccd_case_number should be numeric";

    @Autowired
    private TestConfigProperties testProps;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private BulkScanPaymentTestService bulkScanPaymentTestService;

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
    public void testFieldLevelValidation() throws Exception {
        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(null, null, null, false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(SERVICE_TOKEN, bulkScanCcdPayments);
        bulkScanCcdPaymentsResponse.then().statusCode(BAD_REQUEST.value());

        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(PAYMENT_DCN_MISSING));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(RESPONSIBLE_SERVICE_ID_MISSING));
    }

    @Test()
    public void testNegativeInvalidCcdAndDcnLength() throws Exception {
        String[] dcn = {""};
        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments("", dcn, "AA08", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(SERVICE_TOKEN, bulkScanCcdPayments);
        bulkScanCcdPaymentsResponse.then().statusCode(BAD_REQUEST.value());

        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(INVALID_CCD_REFERENCE_LENGTH));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(INVALID_CCD_REFERENCE_TYPE));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(INVALID_DCN_LENGTH));
    }

    @Test()
    public void testNegativeInvalidSiteId() throws Exception {
        String ccdCaseNumber = "13115656" + RandomUtils.nextInt(10000000, 99999999);
        String[] dcn = {"6200000000001" + RandomUtils.nextInt(10000000, 99999999)};

        BulkScanPaymentRequest bulkScanCcdPayments = createBulkScanCcdPayments(ccdCaseNumber, dcn, "XY123", false);
        Response bulkScanCcdPaymentsResponse = bulkScanPaymentTestService.postBulkScanCcdPayments(SERVICE_TOKEN, bulkScanCcdPayments);
        bulkScanCcdPaymentsResponse.then().statusCode(BAD_REQUEST.value());

        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(INVALID_SITE_ID_LENGTH));
        Assert.assertTrue(bulkScanCcdPaymentsResponse.andReturn().asString().contains(INVALID_SITE_ID));
    }
}
