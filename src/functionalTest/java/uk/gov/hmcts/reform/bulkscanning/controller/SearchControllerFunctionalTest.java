package uk.gov.hmcts.reform.bulkscanning.controller;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.bulkscanning.config.IdamService;
import uk.gov.hmcts.reform.bulkscanning.config.S2sTokenService;
import uk.gov.hmcts.reform.bulkscanning.config.TestConfigProperties;
import uk.gov.hmcts.reform.bulkscanning.config.TestContextConfiguration;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.bulkscanning.config.IdamService.CMC_CITIZEN_GROUP;

@RunWith(SpringIntegrationSerenityRunner.class)
@SpringBootTest
@EnableFeignClients
@ContextConfiguration(classes = TestContextConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yaml")
public class SearchControllerFunctionalTest {

    @Autowired
    private IdamService idamService;

    @Autowired
    private S2sTokenService s2sTokenService;

    @Autowired
    private TestConfigProperties testProps;

    private static String USER_TOKEN;
    private static String SERVICE_TOKEN;
    private static boolean TOKENS_INITIALIZED;

    @Before
    public void setUp() {

        if (!TOKENS_INITIALIZED) {

            USER_TOKEN = idamService.createUserWith(CMC_CITIZEN_GROUP, "citizen").getAuthorisationToken();
            SERVICE_TOKEN = s2sTokenService.getS2sToken(testProps.s2sServiceName, testProps.s2sServiceSecret);
            TOKENS_INITIALIZED = true;

        }
    }

    @Test
    public void test_positive_look_up_of_bulk_scanning_payment_request() throws Exception {
        final String randomDcn = generateRandom(21);
        final String[] dcn = {randomDcn};
        //Given
        final String ccdReference = generateRandom(16);
        createBulkScanPayment(dcn, ccdReference,"AA08", false);
        //When
        Response response = performSearchPaymentByDcn(randomDcn);
        //Then
        System.out.println("The status of the Reference " + response.getBody().prettyPrint());
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals(ccdReference, response.getBody().jsonPath().getString("ccd_reference"));
        assertEquals("AA08", response.getBody().jsonPath().getString("responsible_service_id"));
        assertEquals("INCOMPLETE", response.getBody().jsonPath().getString("all_payments_status"));
    }

    @Test
    public void test_positive_look_up_of_exella_payment_request() throws Exception {
        final String randomDcn = generateRandom(21);
        BulkScanPayment bulkScanPayment = createExellaPaymentRequest(randomDcn,100.00,
                                   "2019-10-31",
                                   123, "GBP","CHEQUE");
        createExellaPayment(bulkScanPayment);
        //When
        Response response = performSearchPaymentByDcn(randomDcn);
        //Then
        System.out.println("The status of the Reference " + response.getBody().prettyPrint());
        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("INCOMPLETE", response.getBody().jsonPath().getString("all_payments_status"));
    }

    @Test
    public void test_bulk_scan_payment_look_up_not_found() throws Exception {
        //Given, When
        Response response = performSearchPaymentByDcn("XXXX11111111111111120");
        //Then
        assertEquals(404, response.getStatusCode());
        System.out.println("The value of the Body " + response.getBody().asString());
    }

    private void createBulkScanPayment(String[] dcn, String ccdReference,
                                       String responsibleServiceId, boolean isExceptionRecord) {

        BulkScanPaymentRequest bulkScanPaymentRequest
            = createBulkScanPaymentRequest(ccdReference,
                                           dcn, responsibleServiceId, isExceptionRecord);
        //Post request
        Response response = RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .body(bulkScanPaymentRequest)
            .contentType(ContentType.JSON)
            .when()
            .post("/bulk-scan-payments");
        assertEquals(201, response.getStatusCode());
    }

    private Response performSearchPaymentByDcn(String randomDCN) {
        return RestAssured.given()
            .header("Authorization", USER_TOKEN)
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .queryParam("document_control_number", randomDCN)
            .contentType(ContentType.JSON)
            .when()
            .get("/cases");
    }

    private void createExellaPayment(BulkScanPayment bulkScanPayment) {
        Response exelaResp = RestAssured.given()
            .header("ServiceAuthorization", SERVICE_TOKEN)
            .contentType(ContentType.JSON)
            .body(bulkScanPayment)
            .when()
            .post("/bulk-scan-payment");
    }


    private static BulkScanPaymentRequest createBulkScanPaymentRequest(final String ccdCaseNumber,
                                                                       final String[] dcn,
                                                                       final String responsibleServiceId,
                                                                       final boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(ResponsibleSiteId.valueOf(responsibleServiceId).toString())
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

    public static BulkScanPayment createExellaPaymentRequest(final String dcnReference,
                                                       final double amount,
                                                       final String bankedDate,
                                                       final int slipNumber,
                                                       final String currencyCode,
                                                       final String paymentMethod) {
        return BulkScanPayment.createPaymentRequestWith()
            .dcnReference(dcnReference)
            .amount(BigDecimal.valueOf(amount))
            .bankedDate(bankedDate)
            .bankGiroCreditSlipNumber(slipNumber)
            .currency(Currency.valueOf(currencyCode).toString())
            .method(PaymentMethod.valueOf(paymentMethod).toString())
            .build();
    }

    public static String generateRandom(int length) {
        Random random = new Random();
        char[] digits = new char[length];
        digits[0] = (char) (random.nextInt(9) + '1');
        for (int i = 1; i < length; i++) {
            digits[i] = (char) (random.nextInt(10) + '0');
        }
        return new String(digits);
    }


}
