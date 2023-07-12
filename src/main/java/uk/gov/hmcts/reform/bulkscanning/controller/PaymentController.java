package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.PaymentResponse;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Optional;

@RestController
@Tag(name = "Bulk Scanning Payment API", description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
    + "payment information contained in the envelope")
    public class PaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Operation(summary = "Get the initial meta data from bulk Scanning" )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bulk Scanning Data retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
        @ApiResponse(responseCode = "401", description = "Failed authentication"),
        @ApiResponse(responseCode = "403", description = "Failed authorization"),
        @ApiResponse(responseCode = "409", description = "Payment DCN already exists")
    })
    @PostMapping("/bulk-scan-payments")
    public ResponseEntity<PaymentResponse> consumeInitialMetaDataBulkScanning(@Valid @RequestBody BulkScanPaymentRequest bsPaymentRequest) {
        LOG.info("Request received from Bulk Scan Payment : {}", bsPaymentRequest);
        return new ResponseEntity<>(PaymentResponse.paymentResponseWith()
                                        .paymentDcns(paymentService.saveInitialMetadataFromBs(bsPaymentRequest))
                                        .build(), HttpStatus.CREATED);
    }

    @Operation(summary = "Provide meta information about the payments contained\n" +
        "in the envelope. This operation will be called after the banking process\n" +
        "has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Bulk Scanning Data retrieved"),
        @ApiResponse(responseCode = "400", description = "Request failed due to malformed syntax"),
        @ApiResponse(responseCode = "401", description = "Failed authentication"),
        @ApiResponse(responseCode = "403", description = "Failed authorization"),
        @ApiResponse(responseCode = "409", description = "Payment DCN already exists")
    })
    @PostMapping("/bulk-scan-payment")
    public ResponseEntity<String> processPaymentFromExela(
        @Valid @RequestBody BulkScanPayment bulkScanPayment) {
        LOG.info("Request received from Exela with Request : {}", bulkScanPayment);
        try {
            LOG.info("Check in Payment metadata for already existing payment from Exela");
            if (Optional.ofNullable(paymentService.getPaymentMetadata(bulkScanPayment.getDcnReference())).isPresent()) {
                LOG.info("Payment already exists for DCN: {}", bulkScanPayment.getDcnReference());
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Payment DCN already exists");
            } else {
                LOG.info("Processing Payment for DCN: {}", bulkScanPayment.getDcnReference());
                paymentService.processPaymentFromExela(bulkScanPayment, bulkScanPayment.getDcnReference());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Created");
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }

    @Operation(summary = "API Endpoint to update case reference for payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns an envelope group id"),
        @ApiResponse(responseCode = "400", description = "Request failed due to malformed syntax"),
        @ApiResponse(responseCode = "401", description = "Failed authentication"),
        @ApiResponse(responseCode = "403", description = "Failed authorisation"),
        @ApiResponse(responseCode = "404", description = "Provided exception reference doesn't exist"),
    })
    @PutMapping("/bulk-scan-payments")
    public ResponseEntity updateCaseReferenceForExceptionRecord(
        @NotEmpty
        @RequestParam("exception_reference")
        @Size(min = 16, max = 16, message = "exception_reference Length must be 16 Characters")
            String exceptionRecordReference,
        @Valid
        @RequestBody
            CaseReferenceRequest caseReferenceRequest) {

        LOG.info(
            "Request received to update case reference {}, for exception record {}",
            caseReferenceRequest,
            exceptionRecordReference
        );
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.updateCaseReferenceForExceptionRecord(
                exceptionRecordReference,
                caseReferenceRequest
            ));
    }

    @Operation(summary ="API Endpoint to mark payment as processed")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns an envelope group id"),
        @ApiResponse(responseCode = "400", description = "Request failed due to malformed syntax"),
        @ApiResponse(responseCode = "401", description = "Failed authentication"),
        @ApiResponse(responseCode = "403", description = "Failed authorisation"),
        @ApiResponse(responseCode = "404", description = "No record exists for provided DCN"),
    })
    @PatchMapping("/bulk-scan-payments/{dcn}/status/{status}")
    public ResponseEntity markPaymentAsProcessed(
        @RequestHeader("Authorization") String authorization,
        @NotEmpty @PathVariable("dcn") String dcn,
        @NotEmpty @PathVariable("status") PaymentStatus status) {
        LOG.info("Request received to mark payment with DCN : {} , status : {}", dcn, status);
        paymentService.updatePaymentStatus(dcn, status);
        return ResponseEntity.status(HttpStatus.OK).body("Updated");
    }

    @Operation(summary = "Delete Bulk scan payment by DCN, Delete Bulk scan payment details for supplied DCN reference")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bulk scan payment deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Bulk scan payment not found for the given DCN")
    })
    @DeleteMapping("/bulk-scan-payment/{dcn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable String dcn) {
        LOG.info("Request received to delete Bulk scan payment for DCN: {}", dcn);
        paymentService.deletePayment(dcn);
    }
}
