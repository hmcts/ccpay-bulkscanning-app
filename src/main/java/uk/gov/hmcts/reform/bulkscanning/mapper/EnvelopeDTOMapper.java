package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleService;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

@Component
public class EnvelopeDTOMapper {

    public Envelope toEnvelopeEntity(EnvelopeDTO envelopeDTO){
        return Envelope.envelopeWith()
            .paymentStatus(envelopeDTO.getPaymentStatus().toString())
            .envelopePayments(toPaymentEntities(envelopeDTO.getPayments()))
            .responsibleServiceId(envelopeDTO.getResponsibleService().toString())
            .build();
    }

    public EnvelopeDTO fromEnvelopeEntity(Envelope envelope){
        return EnvelopeDTO.envelopeDtoWith()
            .id(envelope.getId())
            //.cases(toCaseDTOS(envelope.getCases()))
            //.payments(toPaymentDTOs(envelope.getPayments()))
            .responsibleService(ResponsibleService.valueOf(envelope.getResponsibleServiceId()))
            .paymentStatus(PaymentStatus.valueOf(envelope.getPaymentStatus()))
            .dateCreated(localDateTimeToDate(envelope.getDateCreated()))
            .dateUpdated(localDateTimeToDate(envelope.getDateUpdated()))
            .build();
    }

    public List<CaseDTO> toCaseDTOS(List<EnvelopeCase> caseEntities){
        return caseEntities.stream().map(this::toCaseDTO).collect(Collectors.toList());
    }

    public CaseDTO toCaseDTO(EnvelopeCase caseEntity){
        return CaseDTO.envelopeDtoWith().id(caseEntity.getId())
                                        .ccdReference(caseEntity.getCcdReference())
                                        .exceptionRecordReference(caseEntity.getExceptionRecordReference())
                                        .envelope(fromEnvelopeEntity(caseEntity.getEnvelope()))
                                        .dateCreated(localDateTimeToDate(caseEntity.getDateCreated()))
                                        .dateUpdated(localDateTimeToDate(caseEntity.getDateUpdated()))
                                        .build();
    }

    public List<PaymentDTO> toPaymentDTOs(List<EnvelopePayment> payments){
        return payments.stream().map(this::toPaymentDTO).collect(Collectors.toList());
    }

    public PaymentDTO toPaymentDTO(EnvelopePayment payment){
        return PaymentDTO.paymentDtoWith()
                            .id(payment.getId())
                            .dcnReference(payment.getDcnReference())
                            .envelope(fromEnvelopeEntity(payment.getEnvelope()))
                            .dateCreated(localDateTimeToDate(payment.getDateCreated()))
                            .dateUpdated(localDateTimeToDate(payment.getDateUpdated()))
                            .build();
    }

    public List<EnvelopePayment> toPaymentEntities(List<PaymentDTO> payments){
        return payments.stream().map(this::toPaymentEntity).collect(Collectors.toList());
    }

    public EnvelopePayment toPaymentEntity(PaymentDTO payment){
        return EnvelopePayment.paymentWith()
            .id(payment.getId())
            .dcnReference(payment.getDcnReference())
            .paymentStatus(payment.getPaymentStatus().toString())
            .build();
    }
}
