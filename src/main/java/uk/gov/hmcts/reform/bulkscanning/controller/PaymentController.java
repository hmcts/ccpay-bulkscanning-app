package uk.gov.hmcts.reform.bulkscanning.controller;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDTOMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleService;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    @Autowired
    public PaymentController(PaymentService paymentService,
                             PaymentMetadataDTOMapper paymentMetadataDTOMapper,
                             PaymentDTOMapper paymentDTOMapper){
        this.paymentService = paymentService;
        this.paymentMetadataDTOMapper = paymentMetadataDTOMapper;
        this.paymentDTOMapper = paymentDTOMapper;
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
        @ApiResponse(code = 403, message = "Failed authorisation")
    })
    @PostMapping(value = "/bulk-scan-payment")
    @ResponseBody
    public ResponseEntity<String> createPayment(
        @RequestHeader(value = "ServiceAuthorization") String serviceAuthorization,
        @Valid @RequestBody PaymentRequest paymentRequest) throws Exception {

        // TODO: 07-08-2019 Payment Request Data Validation

        // TODO: 07-08-2019 Insert Payment metadata in BSP DB
        paymentService.createPaymentMetadata(paymentMetadataDTOMapper.fromRequest(paymentRequest));

        // TODO: 07-08-2019 Check for existing DCN in Payment Table Bulk Scan Pay DB,
        Payment payment = paymentService.getPaymentByDcnReference(paymentRequest.getDocument_control_number());

        // TODO: 07-08-2019 if already exists
        // TODO: 07-08-2019 update Payment
        if(null != payment) {
            /*payment.setAmount(paymentRequest.getAmount());
            payment.setCurrency(Currency.valueOf(paymentRequest.getCurrency()));
            payment.setBgcReference(paymentRequest.getBank_giro_credit_slip_number());
            payment.setPaymentMethod(PaymentMethod.valueOf(paymentRequest.getPaymentMethod()));
            payment.setDateBanked(LocalDateTime.ofInstant(paymentRequest.getBanked_date().toInstant(), ZoneId.systemDefault()));

            paymentService.updatePayment(payment);*/

            // TODO: 07-08-2019 Update payment status as complete
            paymentService.createStatusHistory(StatusHistoryDTO.envelopeDtoWith()
                .status(PaymentStatus.COMPLETE)
                .build());
            // TODO: 07-08-2019 Call Payment Service in PayHub to send complete payment details
            // TODO: 07-08-2019 Update payment Status as processed
        /*paymentService.createStatusHistory(StatusHistoryDTO.envelopeDtoWith()
            .status(PaymentStatus.PROGRESSED)
            .build());*/
        }else {
            // TODO: 07-08-2019 Else
            // TODO: 07-08-2019 Create new payment in BSP DB if envelope doesn't exists
            List<PaymentDTO> payments = new ArrayList<PaymentDTO>();
            payments.add(paymentDTOMapper.fromRequest(paymentRequest));

            Envelope envelope = paymentService.createEnvelope(EnvelopeDTO.envelopeDtoWith()
                .payments(payments)
                //Hardcoded for test
                .responsibleService(ResponsibleService.DIVORCE)
                .build());
            EnvelopeDTO envelop = EnvelopeDTO.envelopeDtoWith()
                                        .id(envelope.getId())
                                        .build();

            payments.stream().forEach(paymentDto -> {
                paymentDto.setEnvelope(envelop);
                paymentService.createPayment(paymentDto);
            });

            // TODO: 07-08-2019 Update payment status as incomplete
            paymentService.createStatusHistory(StatusHistoryDTO.envelopeDtoWith()
                .status(PaymentStatus.INCOMPLETE)
                .build());
        }

        return new ResponseEntity<String>(CREATED);
    }
}
