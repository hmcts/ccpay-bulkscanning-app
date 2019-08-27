package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.CaseDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

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

    public EnvelopeDto fromEnvelopeEntity(Envelope envelope) {
        if(Optional.ofNullable(envelope).isPresent()) {
            return EnvelopeDto.envelopeDtoWith()
                .id(envelope.getId())
                .paymentStatus(PaymentStatus.valueOf(envelope.getPaymentStatus()))
                .dateCreated(localDateTimeToDate(envelope.getDateCreated()))
                .dateUpdated(localDateTimeToDate(envelope.getDateUpdated()))
                .build();
        }else {
            return null;
        }
    }

    public List<CaseDto> toCaseDtos(List<EnvelopeCase> caseEntities) {
        return caseEntities.stream().map(this::toCaseDto).collect(Collectors.toList());
    }

    public CaseDto toCaseDto(EnvelopeCase caseEntity) {
        if(Optional.ofNullable(caseEntity).isPresent()) {
            return CaseDto.envelopeDtoWith().id(caseEntity.getId())
                .ccdReference(caseEntity.getCcdReference())
                .exceptionRecordReference(caseEntity.getExceptionRecordReference())
                .envelope(fromEnvelopeEntity(caseEntity.getEnvelope()))
                .dateCreated(localDateTimeToDate(caseEntity.getDateCreated()))
                .dateUpdated(localDateTimeToDate(caseEntity.getDateUpdated()))
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
                .id(payment.getId())
                .dcnReference(payment.getDcnReference())
                .paymentStatus(payment.getPaymentStatus().toString())
                .build();
        }else {
            return null;
        }
    }
}
