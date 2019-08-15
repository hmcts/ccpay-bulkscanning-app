package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.StatusHistoryDTO;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;
import uk.gov.hmcts.reform.bulkscanning.util.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static uk.gov.hmcts.reform.bulkscanning.util.DateUtil.dateToLocalDateTime;

@Component
public class StatusHistoryDTOMapper {

    public StatusHistory toStatusHistoryEntity(StatusHistoryDTO statusHistoryDTO){

        return StatusHistory.statusHistoryWith()
            .status(statusHistoryDTO.getStatus().toString())
            .dateCreated(dateToLocalDateTime(statusHistoryDTO.getDateCreated()))
            .build();
    }
}
