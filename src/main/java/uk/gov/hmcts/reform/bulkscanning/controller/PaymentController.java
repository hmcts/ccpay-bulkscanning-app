package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
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

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);
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
        LOG.info("Request received from Bulk Scan Payment : {}", bsPaymentRequest);
        return new ResponseEntity<>(PaymentResponse.paymentResponseWith()
                                        .paymentDcns(paymentService.saveInitialMetadataFromBs(bsPaymentRequest)
                                                         .getEnvelopePayments().stream()
                                                         .map(payment -> payment.getDcnReference())
                                                         .collect(Collectors.toList()))
                                        .build(), HttpStatus.CREATED);
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
        LOG.info("Request received from Exela with DCN : {} Request : {}", dcnReference, exelaPaymentRequest);
        try {
            LOG.info("Check in Payment metadata for already existing payment from Exela");
            if (Optional.ofNullable(paymentService.getPaymentMetadata(dcnReference)).isPresent()) {
                LOG.info("Payment already exists for DCN: {}", dcnReference);
                return new ResponseEntity<>(HttpStatus.CONFLICT);
            } else {
                LOG.info("Processing Payment for DCN: {}", dcnReference);
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
    @PutMapping("/bulk-scan-cases")
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
    @PatchMapping("/bulk-scan-payments/{dcn}/status/{status}")
    public ResponseEntity markPaymentAsProcessed(@NotEmpty @PathVariable("dcn") String dcn,
                                                 @NotEmpty @PathVariable("status") PaymentStatus status) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(paymentService.updatePaymentStatus(dcn, status));
    }


    @ApiOperation("Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases/{ccd_reference}")
    public ResponseEntity<SearchResponse> retrieveByCCD(@PathVariable("ccd_reference") String ccdReference) {
        LOG.info("Retrieving payments for ccdReference {}: ", ccdReference);
        try {
            SearchResponse searchResponse = paymentService.retrieveByCCDReference(ccdReference);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                LOG.info("Payments Not found for ccdReference : {}", ccdReference);
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
    public ResponseEntity<SearchResponse> retrieveByDCN(@RequestParam("document_control_number")
                                                            String documentControlNumber) {
        LOG.info("Retrieving payments for documentControlNumber : {}", documentControlNumber);
        try {
            SearchResponse searchResponse = paymentService.retrieveByDcn(documentControlNumber);
            if (Optional.ofNullable(searchResponse).isPresent()) {
                LOG.info("SearchResponse : {}", searchResponse);
                return new ResponseEntity<>(searchResponse, HttpStatus.OK);
            } else {
                LOG.info("Payments not found for documentControlNumber : {}", documentControlNumber);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            throw new PaymentException(ex);
        }
    }
}
