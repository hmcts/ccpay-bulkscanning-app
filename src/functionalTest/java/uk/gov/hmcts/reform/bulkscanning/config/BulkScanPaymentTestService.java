package uk.gov.hmcts.reform.bulkscanning.config;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;

@Component
public class BulkScanPaymentTestService {

    public RequestSpecification givenWithServiceAuthHeader(String serviceToken) {
        return SerenityRest.given()
            .header("ServiceAuthorization", serviceToken);
    }

    public RequestSpecification givenWithAuthHeaders(String userToken, String serviceToken) {
        return SerenityRest.given()
            .header("Authorization", userToken)
            .header("ServiceAuthorization", serviceToken);
    }

    public Response postBulkScanDCNPayment(String serviceToken, BulkScanPayment bulkScanPayment) {
        return givenWithServiceAuthHeader(serviceToken)
            .contentType(ContentType.JSON)
            .body(bulkScanPayment)
            .when()
            .post("/bulk-scan-payment");
    }

    public Response postBulkScanCCDPayments(String serviceToken, BulkScanPaymentRequest bulkScanPaymentRequest) {
        return givenWithServiceAuthHeader(serviceToken)
            .contentType(ContentType.JSON)
            .body(bulkScanPaymentRequest)
            .when()
            .post("/bulk-scan-payments");
    }

    public Response updateCaseReferenceForExceptionReference(String serviceToken, String exceptionReference, CaseReferenceRequest caseReferenceRequest) {
        return givenWithServiceAuthHeader(serviceToken)
            .contentType(ContentType.JSON)
            .queryParam("exception_reference", exceptionReference)
            .body(caseReferenceRequest)
            .when()
            .put("/bulk-scan-payments");
    }

    public Response updateBulkScanPaymentStatus(String userToken, String serviceToken, String dcn, String status) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .patch("/bulk-scan-payments/{dcn}/status/{status}", dcn, status);
    }

    public Response getCasesUnprocessedPaymentDetailsByDCN(String userToken, String serviceToken, String dcn) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .queryParam("document_control_number", dcn)
            .when()
            .get("/cases");
    }

    public Response getUnprocessedPaymentDetailsByccdOrExceptionCaseReference(String userToken, String serviceToken, String ccdOrExceptionCaseReference) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/cases/{ccd_reference}", ccdOrExceptionCaseReference);
    }

    public Response getUnprocessedPaymentDetailsByDCN(String userToken, String serviceToken, String dcn) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .get("/case/{dcn}", dcn);
    }

    public Response retrieveReportData(String userToken, String serviceToken, MultiValueMap<String, String> params) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/report/data");
    }

    public Response downloadReport(String userToken, String serviceToken, MultiValueMap<String, String> params) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .contentType(ContentType.JSON)
            .params(params)
            .when()
            .get("/report/download");
    }

    public Response deleteDCNPayment(String userToken, String serviceToken, String dcn) {
        return givenWithAuthHeaders(userToken, serviceToken)
            .when()
            .delete("/bulk-scan-payment/{dcn}", dcn);
    }

}
