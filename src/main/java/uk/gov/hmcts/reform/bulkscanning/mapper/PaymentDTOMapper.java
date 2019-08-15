package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

@Component
public class PaymentDTOMapper {

    public EnvelopePayment toPaymentEntity(PaymentDTO paymentDto){
        return EnvelopePayment.paymentWith()
            .dcnReference(paymentDto.getDcnReference())
            .envelope(Envelope.envelopeWith()
                .id(paymentDto.getEnvelope().getId())
                .paymentStatus(paymentDto.getEnvelope().getPaymentStatus().toString())
                .responsibleServiceId(paymentDto.getEnvelope().getResponsibleService().toString())
                .build())
            .build();
    }

    public PaymentDTO fromRequest(PaymentRequest paymentRequest){
        return PaymentDTO.paymentDtoWith()
            .dcnReference(paymentRequest.getDocument_control_number())
            .paymentStatus(PaymentStatus.INCOMPLETE)
            .build();
    }
}
