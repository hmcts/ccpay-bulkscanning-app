package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "buildWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseReportData {
    private String respServiceId;
    private String paymentMethod;
    private String dateBanked;
}
