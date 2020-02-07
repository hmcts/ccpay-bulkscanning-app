package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "recordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper=false)
public class ReportDataDataLoss extends BaseReportData {
    private String lossResp;
    private String paymentAssetDcn;
    private String respServiceId;
    private String respServiceName;
    private String dateBanked;
    private String bgcBatch;
    private String paymentMethod;
    private BigDecimal amount;
}
