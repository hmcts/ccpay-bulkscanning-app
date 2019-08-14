package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@Api(tags = {"Bulk Scanning Payment API"})
@SwaggerDefinition(tags = {@Tag(name = "BulkScanningController",
    description = "Bulk Scanning Payment API to be used by the scanning supplier to share the "
        + "payment information contained in the envelope")})
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentDTOMapper paymentDTOMapper;
    private final PaymentMetadataDTOMapper paymentMetadataDTOMapper;
    private final EnvelopeDTOMapper envelopeDTOMapper;

    @Autowired
    public PaymentController(PaymentService paymentService,
                             PaymentMetadataDTOMapper paymentMetadataDTOMapper,
                             PaymentDTOMapper paymentDTOMapper,
                             EnvelopeDTOMapper envelopeDTOMapper){
        this.paymentService = paymentService;
        this.paymentMetadataDTOMapper = paymentMetadataDTOMapper;
        this.paymentDTOMapper = paymentDTOMapper;
        this.envelopeDTOMapper = envelopeDTOMapper;
    }

    /*
        POST Endpoint
        "name": "bulk-scanning-payments",
        "description": "API endpoints to support payments via the bulk scanning channel"

        @return Success message & Status code 200 if Payment created
     */
    @ApiOperation(value = "bulk-scan-payments", notes = "Provide meta information about the "
        + "payments contained in the envelope. This operation will be called after the banking "
        + "process has been done and payments have been allocated to a BGC slip / batch")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Returns an envelope group id"),
        @ApiResponse(code = 400, message = "Request failed due to malformed syntax"),
        @ApiResponse(code = 401, message = "Failed authentication"),
        @ApiResponse(code = 403, message = "Failed authorisation"),
        @ApiResponse(code = 404, message = "Payment not Found")
    })
    @PostMapping(value = "/bulk-scan-payment")
    @ResponseBody
    public ResponseEntity<String> createPayment(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Validated @RequestBody PaymentRequest paymentRequest) throws Exception {

        // TODO: 07-08-2019 Payment Request Data Validation

        // TODO: 07-08-2019 Insert Payment metadata in BSP DB
        paymentService.createPaymentMetadata(paymentMetadataDTOMapper.fromRequest(paymentRequest));

        // TODO: 07-08-2019 Check for existing DCN in Payment Table Bulk Scan Pay DB,
        Payment payment = paymentService.getPaymentByDcnReference(paymentRequest.getDocument_control_number());

        // TODO: 07-08-2019 if already exists
        // TODO: 07-08-2019 update Payment
        if(null != payment) {
            // TODO: 07-08-2019 Update payment status as complete
            payment.setPaymentStatus(PaymentStatus.COMPLETE);
            payment.setDateUpdated(LocalDateTime.now());
            paymentService.updatePayment(payment);

            paymentService.updateEnvelopePaymentStatus(payment.getEnvelope());
            // TODO: 07-08-2019 Call Payment Service in PayHub to send complete payment details
            // TODO: 07-08-2019 Update payment Status as processed
        }else {
            // TODO: 07-08-2019 Else
            // TODO: 07-08-2019 Create new payment in BSP DB if envelope doesn't exists
            List<PaymentDTO> payments = new ArrayList<PaymentDTO>();
            payments.add(paymentDTOMapper.fromRequest(paymentRequest));

            Envelope envelope = paymentService.createEnvelope(EnvelopeDTO.envelopeDtoWith()
                                                                            .paymentStatus(PaymentStatus.INCOMPLETE)
                                                                            .payments(payments)
                                                                            .build());
            // TODO: 07-08-2019 Update payment status as incomplete
            paymentService.updateEnvelopePaymentStatus(envelope);
        }
        return new ResponseEntity<>(CREATED);
    }
}
