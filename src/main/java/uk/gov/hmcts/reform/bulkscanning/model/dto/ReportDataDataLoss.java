package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "recordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportDataDataLoss {
    private String lossResp;
    private String paymentAssetDcn;
    private String respServiceId;
    private String respServiceName;
    private String dateBanked;
    private String bgcBatch;
    private String paymentMethod;
    private BigDecimal amount;
}
