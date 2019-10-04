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
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "recordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportData {
    @Builder.Default
    private String exceptionRef = StringUtils.EMPTY;
    @Builder.Default
    private String ccdRef = StringUtils.EMPTY;
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
    private String paymentAssetDcn = StringUtils.EMPTY;
    @Builder.Default
    private BigDecimal amount = BigDecimal.ZERO;
    @Builder.Default
    private String lossResp = StringUtils.EMPTY;
}
