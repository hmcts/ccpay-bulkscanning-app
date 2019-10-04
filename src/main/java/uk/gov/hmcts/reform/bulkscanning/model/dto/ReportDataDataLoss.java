package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "recordWith")
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportDataDataLoss {
    @Builder.Default
    private String lossResp = StringUtils.EMPTY;
    @Builder.Default
    private String paymentAssetDcn = StringUtils.EMPTY;
    @Builder.Default
    private String respServiceId = StringUtils.EMPTY;
    @Builder.Default
    private String respServiceName = StringUtils.EMPTY;
    @Builder.Default
    private String dateBanked = StringUtils.EMPTY;
    @Builder.Default
    private String bgcBatch = StringUtils.EMPTY;
    @Builder.Default
    private String paymentMethod = StringUtils.EMPTY;
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
}
