package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;

@Component
public class PaymentDtoMapper {

    public EnvelopePayment toPaymentEntity(PaymentDto paymentDto) {
        return EnvelopePayment.paymentWith()
            .dcnReference(paymentDto.getDcnReference())
            .envelope(Envelope.envelopeWith()
                          .id(paymentDto.getEnvelope().getId())
                          .paymentStatus(paymentDto.getEnvelope().getPaymentStatus().toString())
                          .build())
            .build();
    }

    public PaymentDto fromRequest(PaymentRequest paymentRequest, String dcnReference) {
        return PaymentDto.paymentDtoWith()
            .dcnReference(dcnReference)
            .paymentStatus(PaymentStatus.INCOMPLETE)
            .build();
    }
}
