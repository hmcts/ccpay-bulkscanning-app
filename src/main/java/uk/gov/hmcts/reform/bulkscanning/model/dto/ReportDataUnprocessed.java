package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(builderMethodName = "recordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper=false)
public class ReportDataUnprocessed extends BaseReportData {

    private String respServiceName;
    private String exceptionRef;
    private String ccdRef;
    private String bgcBatch;
    private String paymentAssetDcn;
    private BigDecimal amount;
}
