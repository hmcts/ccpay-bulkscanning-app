package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EnvelopeDtoMapper {

    public Envelope toEnvelopeEntity(EnvelopeDto envelopeDto) {
        if(Optional.ofNullable(envelopeDto).isPresent()) {
            return Envelope.envelopeWith()
                .paymentStatus(envelopeDto.getPaymentStatus().toString())
                .envelopePayments(toPaymentEntities(envelopeDto.getPayments()))
                .build();
        }else {
            return null;
        }
    }

    public List<EnvelopePayment> toPaymentEntities(List<PaymentDto> payments) {
        return payments.stream().map(this::toPaymentEntity).collect(Collectors.toList());
    }

    public EnvelopePayment toPaymentEntity(PaymentDto payment) {
        if(Optional.ofNullable(payment).isPresent()) {
            return EnvelopePayment.paymentWith()
                .dcnReference(payment.getDcnReference())
                .source(payment.getSource())
                .paymentStatus(payment.getPaymentStatus().toString())
                .build();
        }else {
            return null;
        }
    }
}
