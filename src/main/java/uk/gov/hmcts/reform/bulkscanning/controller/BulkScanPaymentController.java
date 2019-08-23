package uk.gov.hmcts.reform.bulkscanning.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.SearchRequest;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.BulkScanConsumerService;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BSPaymentController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class BulkScanPaymentController {

    @Autowired
    PaymentService paymentService;

    @Autowired
    BulkScanConsumerService bsConsumerService;

    @Autowired
    PaymentDtoMapper paymentDtoMapper;

    @Autowired
    PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    @Autowired
    public BulkScanPaymentController(PaymentService paymentService,
                                     BulkScanConsumerService bsConsumerService,
                                     PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                                     PaymentDtoMapper paymentDtoMapper) {
        this.paymentService = paymentService;
        this.bsConsumerService = bsConsumerService;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.paymentDtoMapper = paymentDtoMapper;
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
    public ResponseEntity consumeInitialMetaDataBulkScanning(@Valid @RequestBody BulkScanPaymentRequest bsPaymentRequest) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(bsConsumerService.saveInitialMetadataFromBs(bsPaymentRequest));
    }

    @ApiOperation("Provide meta information about the "
        + "payments contained in the envelope. This operation will be called after the banking "
        + "process has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 409, message = "Conflict")
    })
    @PutMapping("/bulk-scan-payments/{document_control_number}")
    @Transactional
    public ResponseEntity<String> processPaymentFromExela(
        @PathVariable("document_control_number") String dcnReference,
        @Validated @RequestBody PaymentRequest paymentRequest) {

        try {
            // TODO: 22/08/2019 Hardcoded for Testing only
           /* if (StringUtils.isNotEmpty(serviceAuthorization)
                && !"AUTH123".equalsIgnoreCase(serviceAuthorization)) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }*/
           /* Commented Service Auth Code*/
            if (StringUtils.isNotEmpty(dcnReference)) {
                //Check in Payment metadata for already existing payment from Exela
                if (paymentService.getPaymentMetadata(dcnReference) == null) {
                    processPaymentFromExela(paymentRequest, dcnReference);
                } else {
                    return new ResponseEntity<>(HttpStatus.CONFLICT);
                }
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Document_Control_Number missing in Exela Request"
                );
            }
        } catch (PaymentException pex) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "API Failed with Exception!!!", pex);
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
    @PutMapping("/bulk-scan-cases/")
    public ResponseEntity updateCaseReferenceForExceptionRecord(@NotEmpty @RequestParam("exception_reference") String exceptionRecordReference,
                                                                @Valid @RequestBody CaseReferenceRequest caseReferenceRequest) {

        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(bsConsumerService.updateCaseReferenceForExceptionRecord(exceptionRecordReference, caseReferenceRequest));
    }

    @ApiOperation("API Endpoint to mark payment as processed")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "No record exists for provided DCN"),
    })
    @PatchMapping("/bulk-scan-payments/{dcn}/PROCESS")
    public ResponseEntity markPaymentAsProcessed(@NotEmpty @PathVariable("dcn") String dcn) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .contentType(MediaType.APPLICATION_JSON)
            .body(bsConsumerService.markPaymentAsProcessed(dcn));
    }

    @ApiOperation("Case with unprocessed payments details by CCD Case Reference/Exception Record")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases/{ccd_reference}")
    public ResponseEntity<String> retrieveByCCD(@PathVariable("ccd_reference") String ccdReference) throws JsonProcessingException {
        EnvelopeCase envelopeCase = paymentService.getEnvelopeCaseByCCDReference(SearchRequest.searchRequestWith()
                                                                                     .ccdReference(ccdReference)
                                                                                     .exceptionRecord(ccdReference)
                                                                                     .build());
        List<PaymentMetadata> paymentMetadataList = new ArrayList<>();
        if (envelopeCase == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else {
            envelopeCase.getEnvelope().getEnvelopePayments().stream().forEach(envelopePayment -> {
                paymentMetadataList.add(paymentService.getPaymentMetadata(envelopePayment.getDcnReference()));
            });
            SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference(envelopeCase.getCcdReference())
                .exceptionRecordReference(envelopeCase.getExceptionRecordReference())
                .payments(paymentMetadataDtoMapper.fromPaymentMetadataEntities(paymentMetadataList))
                .build();
            return new ResponseEntity<>(new ObjectMapper().writeValueAsString(searchResponse), HttpStatus.OK);
        }
    }

    @ApiOperation("Case with unprocessed payment details by Payment DCN")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Payments retrieved"),
        @ApiResponse(code = 404, message = "Payments not found")
    })
    @GetMapping("/cases")
    public ResponseEntity<String> retrieveByDCN(@RequestParam(required = true, value = "document_control_number")
                                                    String documentControlNumber) throws JsonProcessingException {

        EnvelopeCase envelopeCase = paymentService.getEnvelopeCaseByDCN(SearchRequest.searchRequestWith()
                                                                            .documentControlNumber(
                                                                                documentControlNumber)
                                                                            .build());
        List<PaymentMetadata> paymentMetadataList = new ArrayList<>();
        if (envelopeCase == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else {
            envelopeCase.getEnvelope().getEnvelopePayments().stream().forEach(envelopePayment -> {
                paymentMetadataList.add(paymentService.getPaymentMetadata(envelopePayment.getDcnReference()));
            });
            SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference(envelopeCase.getCcdReference())
                .exceptionRecordReference(envelopeCase.getExceptionRecordReference())
                .payments(paymentMetadataDtoMapper.fromPaymentMetadataEntities(paymentMetadataList))
                .build();

            return new ResponseEntity<>(new ObjectMapper().writeValueAsString(searchResponse), HttpStatus.OK);
        }
    }

    private void processPaymentFromExela(PaymentRequest paymentRequest, String dcnReference) {
        //Insert Payment metadata in BSP DB
        paymentService.createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(paymentRequest, dcnReference));

        //Check for existing DCN in Payment Table Bulk Scan Pay DB,
        EnvelopePayment payment = paymentService.getPaymentByDcnReference(dcnReference);

        if (null == payment) {
            //Create new payment in BSP DB if envelope doesn't exists
            List<PaymentDto> payments = new ArrayList<>();
            payments.add(paymentDtoMapper.fromRequest(paymentRequest, dcnReference));

            Envelope envelope = paymentService.createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                                  .paymentStatus(INCOMPLETE)
                                                                  .payments(payments)
                                                                  .build());
            //Update payment status as incomplete
            paymentService.updateEnvelopePaymentStatus(envelope);
        } else {
            if (payment.getEnvelope().getPaymentStatus().equalsIgnoreCase(INCOMPLETE.toString())) {
                //07-08-2019 Update payment status as complete
                payment.setPaymentStatus(COMPLETE.toString());
                payment.setDateUpdated(LocalDateTime.now());
                paymentService.updatePayment(payment);

                paymentService.updateEnvelopePaymentStatus(payment.getEnvelope());
            }
        }
    }
}