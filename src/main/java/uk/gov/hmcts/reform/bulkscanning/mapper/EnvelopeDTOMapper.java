package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Case;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EnvelopeDTOMapper {

    public Envelope toEnvelopeEntity(EnvelopeDTO envelopeDTO){
        return Envelope.envelopeWith()
            .paymentStatus(envelopeDTO.getPaymentStatus())
            .payments(toPaymentEntities(envelopeDTO.getPayments()))
            .responsibleService(envelopeDTO.getResponsibleService())
            .dateCreated(dateToLocalDateTime(envelopeDTO.getDateCreated()))
            .dateUpdated(dateToLocalDateTime(envelopeDTO.getDateUpdated()))
            .build();
    }

    public EnvelopeDTO fromEnvelopeEntity(Envelope envelope){
        return EnvelopeDTO.envelopeDtoWith()
            .id(envelope.getId())
            //.cases(toCaseDTOS(envelope.getCases()))
            .payments(toPaymentDTOs(envelope.getPayments()))
            .responsibleService(envelope.getResponsibleService())
            .paymentStatus(envelope.getPaymentStatus())
            .dateCreated(localDateTimeToDate(envelope.getDateCreated()))
            .dateUpdated(localDateTimeToDate(envelope.getDateUpdated()))
            .build();
    }

    public List<CaseDTO> toCaseDTOS(List<Case> caseEntities){
        return caseEntities.stream().map(this::toCaseDTO).collect(Collectors.toList());
    }

    public CaseDTO toCaseDTO(Case caseEntity){
        return CaseDTO.envelopeDtoWith().id(caseEntity.getId())
                                        .ccdReference(caseEntity.getCcdReference())
                                        .exceptionRecordReference(caseEntity.getExceptionRecordReference())
                                        .envelope(fromEnvelopeEntity(caseEntity.getEnvelope()))
                                        .dateCreated(localDateTimeToDate(caseEntity.getDateCreated()))
                                        .dateUpdated(localDateTimeToDate(caseEntity.getDateUpdated()))
                                        .build();
    }

    public List<PaymentDTO> toPaymentDTOs(List<Payment> payments){
        return payments.stream().map(this::toPaymentDTO).collect(Collectors.toList());
    }

    public PaymentDTO toPaymentDTO(Payment payment){
        return PaymentDTO.paymentDtoWith()
                            .id(payment.getId())
                            .dcnReference(payment.getDcnReference())
                            .envelope(fromEnvelopeEntity(payment.getEnvelope()))
                            .dateCreated(localDateTimeToDate(payment.getDateCreated()))
                            .dateUpdated(localDateTimeToDate(payment.getDateUpdated()))
                            .build();
    }

    public List<Payment> toPaymentEntities(List<PaymentDTO> payments){
        return payments.stream().map(this::toPaymentEntity).collect(Collectors.toList());
    }

    public Payment toPaymentEntity(PaymentDTO payment){
        return Payment.paymentWith()
            .id(payment.getId())
            .dcnReference(payment.getDcnReference())
            .paymentStatus(payment.getPaymentStatus())
            .dateCreated(dateToLocalDateTime(payment.getDateCreated()))
            .dateUpdated(dateToLocalDateTime(payment.getDateUpdated()))
            .build();
    }

    public Date localDateTimeToDate(LocalDateTime ldt){
        return ldt != null ? Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()) : null;
    }

    public LocalDateTime dateToLocalDateTime(Date date){
        return date != null ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()) : null;
    }
}
