package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.time.LocalDateTime;

@Component
public class PaymentDTOMapper {

    public Payment toPaymentEntity(PaymentDTO paymentDto){
        return Payment.paymentWith()
            .dcnReference(paymentDto.getDcnReference())
            .envelope(Envelope.envelopeWith()
                .id(paymentDto.getEnvelope().getId())
                .paymentStatus(paymentDto.getEnvelope().getPaymentStatus())
                .responsibleService(paymentDto.getEnvelope().getResponsibleService())
                .build())
            .dateCreated(LocalDateTime.now())
            .build();
    }

    public PaymentDTO fromRequest(PaymentRequest paymentRequest){
        return PaymentDTO.paymentDtoWith()
            .dcnReference(paymentRequest.getDocument_control_number())
            .build();
    }
}
