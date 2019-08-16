package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

@Component
public class StatusHistoryDtoMapper {

    public StatusHistory toStatusHistoryEntity(StatusHistoryDto statusHistoryDto) {

        return StatusHistory.statusHistoryWith()
            .status(statusHistoryDto.getStatus().toString())
            .build();
    }
}
