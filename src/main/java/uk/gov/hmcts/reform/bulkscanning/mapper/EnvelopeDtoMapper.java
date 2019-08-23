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
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

@Component
public class EnvelopeDtoMapper {

    public Envelope toEnvelopeEntity(EnvelopeDto envelopeDto) {
        return Envelope.envelopeWith()
            .paymentStatus(envelopeDto.getPaymentStatus().toString())
            .envelopePayments(toPaymentEntities(envelopeDto.getPayments()))
            .build();
    }

    public EnvelopeDto fromEnvelopeEntity(Envelope envelope) {
        return EnvelopeDto.envelopeDtoWith()
            .id(envelope.getId())
            .paymentStatus(PaymentStatus.valueOf(envelope.getPaymentStatus()))
            .dateCreated(localDateTimeToDate(envelope.getDateCreated()))
            .dateUpdated(localDateTimeToDate(envelope.getDateUpdated()))
            .build();
    }

    public List<CaseDto> toCaseDtos(List<EnvelopeCase> caseEntities) {
        return caseEntities.stream().map(this::toCaseDto).collect(Collectors.toList());
    }

    public CaseDto toCaseDto(EnvelopeCase caseEntity) {
        return CaseDto.envelopeDtoWith().id(caseEntity.getId())
            .ccdReference(caseEntity.getCcdReference())
            .exceptionRecordReference(caseEntity.getExceptionRecordReference())
            .envelope(fromEnvelopeEntity(caseEntity.getEnvelope()))
            .dateCreated(localDateTimeToDate(caseEntity.getDateCreated()))
            .dateUpdated(localDateTimeToDate(caseEntity.getDateUpdated()))
            .build();
    }

    public List<EnvelopePayment> toPaymentEntities(List<PaymentDto> payments) {
        return payments.stream().map(this::toPaymentEntity).collect(Collectors.toList());
    }

    public EnvelopePayment toPaymentEntity(PaymentDto payment) {
        return EnvelopePayment.paymentWith()
            .id(payment.getId())
            .dcnReference(payment.getDcnReference())
            .paymentStatus(payment.getPaymentStatus().toString())
            .build();
    }
}