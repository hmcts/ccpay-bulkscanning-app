package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDto;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleService;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

@Component
public class EnvelopeDtoMapper {

    public Envelope toEnvelopeEntity(EnvelopeDto envelopeDto) {
        return Envelope.envelopeWith()
            .paymentStatus(envelopeDto.getPaymentStatus().toString())
            .envelopePayments(toPaymentEntities(envelopeDto.getPayments()))
            .responsibleServiceId(envelopeDto.getResponsibleService().toString())
            .build();
    }

    public EnvelopeDto fromEnvelopeEntity(Envelope envelope) {
        return EnvelopeDto.envelopeDtoWith()
            .id(envelope.getId())
            //.cases(toCaseDTOS(envelope.getCases()))
            //.payments(toPaymentDTOs(envelope.getPayments()))
            .responsibleService(ResponsibleService.valueOf(envelope.getResponsibleServiceId()))
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

    public List<PaymentDto> toPaymentDtos(List<EnvelopePayment> payments) {
        return payments.stream().map(this::toPaymentDto).collect(Collectors.toList());
    }

    public PaymentDto toPaymentDto(EnvelopePayment payment) {
        return PaymentDto.paymentDtoWith()
            .id(payment.getId())
            .dcnReference(payment.getDcnReference())
            .envelope(fromEnvelopeEntity(payment.getEnvelope()))
            .dateCreated(localDateTimeToDate(payment.getDateCreated()))
            .dateUpdated(localDateTimeToDate(payment.getDateUpdated()))
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
