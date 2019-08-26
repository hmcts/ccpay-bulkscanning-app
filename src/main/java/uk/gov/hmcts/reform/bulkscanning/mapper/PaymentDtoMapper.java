package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

@Component
public class PaymentDtoMapper {

    public EnvelopePayment toPaymentEntity(PaymentDto paymentDto) {
        if(Optional.ofNullable(paymentDto).isPresent()){
            return EnvelopePayment.paymentWith()
                .dcnReference(paymentDto.getDcnReference())
                .envelope(Envelope.envelopeWith()
                              .id(paymentDto.getEnvelope().getId())
                              .paymentStatus(paymentDto.getEnvelope().getPaymentStatus().toString())
                              .build())
                .build();
        }else {
            return null;
        }
    }

    public PaymentDto fromRequest(PaymentRequest paymentRequest, String dcnReference) {
        if(Optional.ofNullable(paymentRequest).isPresent()) {
            return PaymentDto.paymentDtoWith()
                .dcnReference(dcnReference)
                .paymentStatus(PaymentStatus.INCOMPLETE)
                .build();
        }else {
            return null;
        }
    }

    public List<PaymentDto> fromPaymentsEntity(List<EnvelopePayment> payments) {
        return payments.stream().map(this::fromPaymentEntity).collect(Collectors.toList());
    }

    public PaymentDto fromPaymentEntity(EnvelopePayment payment) {
        if(Optional.ofNullable(payment).isPresent()) {
            return PaymentDto.paymentDtoWith()
                .id(payment.getId())
                .dcnReference(payment.getDcnReference())
                .dateCreated(localDateTimeToDate(payment.getDateCreated()))
                .dateUpdated(localDateTimeToDate(payment.getDateUpdated()))
                .build();
        }else {
            return null;
        }
    }
}
