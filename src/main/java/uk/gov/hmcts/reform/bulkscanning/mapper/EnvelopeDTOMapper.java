package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.EnvelopeDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.time.LocalDateTime;

@Component
public class EnvelopeDTOMapper {

    public Envelope toEnvelopeEntity(EnvelopeDTO envelopeDTO){
        return Envelope.envelopeWith()
            .responsibleService(envelopeDTO.getResponsibleService())
            .dateCreated(LocalDateTime.now())
            .build();
    }
}
