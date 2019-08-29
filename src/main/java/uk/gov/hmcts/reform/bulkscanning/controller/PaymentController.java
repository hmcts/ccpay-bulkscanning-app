package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.ExelaPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BSPaymentController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @ApiOperation(value = "Get the initial meta data from bulk Scanning",
        notes = "Get the initial meta data from bulk Scanning")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Bulk Scanning Data retrieved"),
        @ApiResponse(code = 400, message = "Bad request"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorization")
    })
    @PostMapping("/bulk-scan-payments")
    public ResponseEntity<PaymentResponse> consumeInitialMetaDataBulkScanning(@Valid @RequestBody BulkScanPaymentRequest bsPaymentRequest) {
        PaymentResponse response = PaymentResponse.paymentResponseWith()
            .paymentDCNs(paymentService.saveInitialMetadataFromBs(bsPaymentRequest)
                             .getEnvelopePayments().stream()
                             .map(payment -> payment.getDcnReference())
                             .collect(Collectors.toList()))
            .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @ApiOperation("This operation will be called after the banking "
        + "process has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 409, message = "Conflict")
    })
    @PutMapping("/bulk-scan-payments/{document_control_number}")
    public ResponseEntity<String> processPaymentFromExela(
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @PathVariable("document_control_number") String dcnReference,
        @Valid @RequestBody ExelaPaymentRequest exelaPaymentRequest) {
        try {
            //Check in Payment metadata for already existing payment from Exela
            if (Optional.ofNullable(paymentService.getPaymentMetadata(dcnReference)).isPresent()) {
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            } else {
                paymentService.processPaymentFromExela(exelaPaymentRequest, dcnReference);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("API Endpoint to update case reference for payment")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "Provided exception reference doesn't exist"),
    })
    @PutMapping("/bulk-scan-payments")
    public ResponseEntity updateCaseReferenceForExceptionRecord(@NotEmpty @RequestParam("exception_reference") String exceptionRecordReference,
                                                                @Valid @RequestBody CaseReferenceRequest caseReferenceRequest) {

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.updateCaseReferenceForExceptionRecord(
                exceptionRecordReference,
                caseReferenceRequest
            ));
    }

    @ApiOperation("API Endpoint to mark payment as processed")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "No record exists for provided DCN"),
    })
    @PatchMapping("/bulk-scan-payments/{dcn}/process")
    public ResponseEntity markPaymentAsProcessed(@NotEmpty @PathVariable("dcn") String dcn) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.markPaymentAsProcessed(dcn));
    }

    @ApiOperation("Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases/{ccd_reference}")
    public ResponseEntity<SearchResponse> retrieveByCCD(@PathVariable("ccd_reference") String ccdReference) {
        try {
            SearchResponse searchResponse = paymentService.retrieveByCCDReference(ccdReference);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @ApiOperation("Case with unprocessed payment details by Payment DCN")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases")
    public ResponseEntity<SearchResponse> retrieveByDCN(@RequestParam(required = true, value = "document_control_number")
                                                            String documentControlNumber) {
        try {
            SearchResponse searchResponse = paymentService.retrieveByDcn(documentControlNumber);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }
}
