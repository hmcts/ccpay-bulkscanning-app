package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.StatusHistoryDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

import java.util.Optional;

@Component
public class StatusHistoryDtoMapper {
    public StatusHistory toStatusHistoryEntity(StatusHistoryDto statusHistoryDto) {
        if(Optional.ofNullable(statusHistoryDto).isPresent()){
            return StatusHistory.statusHistoryWith()
                .status(statusHistoryDto.getStatus().toString())
                .build();
        }else {
            return null;
        }
    }
}
