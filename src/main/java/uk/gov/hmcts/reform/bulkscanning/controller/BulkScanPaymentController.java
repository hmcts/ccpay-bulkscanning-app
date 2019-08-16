package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleService;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BSPaymentController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class BulkScanPaymentController {

    private final PaymentService paymentService;
    private final PaymentDtoMapper paymentDtoMapper;
    private final PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    @Autowired
    public BulkScanPaymentController(PaymentService paymentService,
                                     PaymentMetadataDtoMapper paymentMetadataDtoMapper,
                                     PaymentDtoMapper paymentDtoMapper) {
        this.paymentService = paymentService;
        this.paymentMetadataDtoMapper = paymentMetadataDtoMapper;
        this.paymentDtoMapper = paymentDtoMapper;
    }

    /*
        POST Endpoint
        "name": "bulk-scanning-payments",
        "description": "API endpoints to support payments via the bulk scanning channel"

        @return Success message & Status code 200 if Payment created
     */
    @ApiOperation("Provide meta information about the "
        + "payments contained in the envelope. This operation will be called after the banking "
        + "process has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "Payment not Found")
    })
    @PutMapping("/bulk-scan-payment")
    @Transactional
    public ResponseEntity<String> createPayment(
        @RequestHeader("ServiceAuthorization") String serviceAuthorization,
        @Validated @RequestBody PaymentRequest paymentRequest) {

        try {
            //Insert Payment metadata in BSP DB
            paymentService.createPaymentMetadata(paymentMetadataDtoMapper.fromRequest(paymentRequest));

            //Check for existing DCN in Payment Table Bulk Scan Pay DB,
            EnvelopePayment payment = paymentService.getPaymentByDcnReference(paymentRequest.getDocumentControlNumber());

            if (null == payment) {
                //Create new payment in BSP DB if envelope doesn't exists
                List<PaymentDto> payments = new ArrayList<>();
                payments.add(paymentDtoMapper.fromRequest(paymentRequest));

                Envelope envelope = paymentService.createEnvelope(EnvelopeDto.envelopeDtoWith()
                                                                      //tobe removed : Hardcoded for Testing
                                                                      .responsibleService(ResponsibleService.DIVORCE)
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
                } else {
                    return new ResponseEntity<>(HttpStatus.CONFLICT);
                }
            }
            return new ResponseEntity<>(CREATED);
        } catch (PaymentException pex) {
            throw new ResponseStatusException(HttpStatus.EXPECTATION_FAILED, "API Failed with Exception!!!", pex);
        }
    }
}
