package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

import java.time.LocalDateTime;

@Component
public class StatusHistoryDTOMapper {

    public StatusHistory toStatusHistoryEntity(StatusHistoryDTO statusHistoryDTO){

        return StatusHistory.statusHistoryWith()
            .status(statusHistoryDTO.getStatus())
            .dateCreated(LocalDateTime.now())
            .build();
    }
}