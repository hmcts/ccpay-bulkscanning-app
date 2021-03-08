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
import org.springframework.test.context.junit4.SpringRunner;
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

    private static String USER_TOKEN="eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiMWVyMFdSd2dJT1RBRm9qRTRyQy9mYmVLdTNJPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJraXNoYW5raUBnbWFpbC5jb20iLCJjdHMiOiJPQVVUSDJfU1RBVEVMRVNTX0dSQU5UIiwiYXV0aF9sZXZlbCI6MCwiYXVkaXRUcmFja2luZ0lkIjoiNDFmNjNkMmEtNjFlNS00YTVmLThhZjgtMTUwNTE0NDBhODY4LTU4Mjc1NjIiLCJpc3MiOiJodHRwczovL2Zvcmdlcm9jay1hbS5zZXJ2aWNlLmNvcmUtY29tcHV0ZS1pZGFtLWFhdDIuaW50ZXJuYWw6ODQ0My9vcGVuYW0vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9obWN0cyIsInRva2VuTmFtZSI6ImFjY2Vzc190b2tlbiIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJhdXRoR3JhbnRJZCI6IkVmM1BMY0k3Y0swOHdFN0xXUDgzR2JpaWhNTSIsImF1ZCI6InBheWJ1YmJsZSIsIm5iZiI6MTYxNDc5OTU1OSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInNjb3BlIjpbIm9wZW5pZCIsInByb2ZpbGUiLCJyb2xlcyJdLCJhdXRoX3RpbWUiOjE2MTQ3OTk1NTgsInJlYWxtIjoiL2htY3RzIiwiZXhwIjoxNjE0ODI4MzU5LCJpYXQiOjE2MTQ3OTk1NTksImV4cGlyZXNfaW4iOjI4ODAwLCJqdGkiOiJOTXdRVHRLRjlsS2czWkg0cWJsb1labjVISmcifQ.GIo0hzvA2do_zNi8litkATIpSFIVgp2-EiFn_W9YonMlXT_ASjJ-HBUlPzeklomvwJOWg-aC_5dqRW1z_omeY7gB_5i2_Piycaj21yBe-fsSIIP7hq3VI70A34BPNUiD-J6MtJG0qxwBRSwWeI1LgtnpYPQ3-QRArvZGTbso0-bzqgttrpav1OMhVV-CPXra5PJTQsiOuAEFU_lw8pxdZdZgPngZJe9OXYUYVnszzTEk_FDnH1uesmjoKDAQKl5pK8JEk26q_QRXERfKW6QW6biYZks9FKs0LBI1Za-Vc9Hd0gpfl4qVnjlNA2dqfZ1OsNLVrsG_isRwSa9Q2R94yA";
    private static String SERVICE_TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhcGlfZ3ciLCJleHAiOjE2MTQ4MTM5ODJ9.n2iyFWqpamn8FJTDS1kXGvEvCBw3n8KYt1tFR0JG7Y2YxkZ8_iLN0UiFcsX0oS9n8AjSe733WB358rp0MrhiEQ";
    private static boolean TOKENS_INITIALIZED = true;

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
