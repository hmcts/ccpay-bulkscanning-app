package uk.gov.hmcts.reform.bulkscanning.validator;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerFunctionalTest.createBulkScanPaymentRequest;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@EnableFeignClients
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class BulkScanValidatorFunctionalTest {

    public static final String RESPONSIBLE_SERVICE_ID_MISSING = "site_id can't be Blank";
    public static final String CCD_REFERENCE_MISSING = "ccd_case_number can't be Blank";
    public static final String PAYMENT_DCN_MISSING = "document_control_numbers can't be Blank";

    @Autowired
    private TestConfigProperties testProps;

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
    public void testFieldLevelValidation() throws Exception {
        String[] dcn = {""};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(null, null,
                                                                                     "AA08", false);

        Response response = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(bulkScanPaymentRequest)
            .contentType(ContentType.JSON)
            .when()
            .post("/bulk-scan-payments");

        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(response.getStatusCode()));

        Assert.assertTrue(response.andReturn().asString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(response.andReturn().asString().contains(PAYMENT_DCN_MISSING));
    }

    @Test()
    @Transactional
    public void testFieldLevelValidationAA09() throws Exception {
        String[] dcn = {""};
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(null, null,
                                                                                     "AA09", false);

        Response response = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(bulkScanPaymentRequest)
            .contentType(ContentType.JSON)
            .when()
            .post("/bulk-scan-payments");

        Assert.assertEquals(Integer.valueOf(400), Integer.valueOf(response.getStatusCode()));

        Assert.assertTrue(response.andReturn().asString().contains(CCD_REFERENCE_MISSING));
        Assert.assertTrue(response.andReturn().asString().contains(PAYMENT_DCN_MISSING));
    }
}
