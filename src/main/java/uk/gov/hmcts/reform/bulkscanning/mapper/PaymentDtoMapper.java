package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

@Component
public class PaymentDtoMapper {

    public EnvelopePayment toPaymentEntity(PaymentDto paymentDto) {
        return EnvelopePayment.paymentWith()
            .dcnReference(paymentDto.getDcnReference())
            .envelope(Envelope.envelopeWith()
                          .id(paymentDto.getEnvelope().getId())
                          .paymentStatus(paymentDto.getEnvelope().getPaymentStatus().toString())
                          .responsibleServiceId(paymentDto.getEnvelope().getResponsibleService().toString())
                          .build())
            .build();
    }

    public PaymentDto fromRequest(PaymentRequest paymentRequest) {
        return PaymentDto.paymentDtoWith()
            .dcnReference(paymentRequest.getDocumentControlNumber())
            .paymentStatus(PaymentStatus.INCOMPLETE)
            .build();
    }
}
