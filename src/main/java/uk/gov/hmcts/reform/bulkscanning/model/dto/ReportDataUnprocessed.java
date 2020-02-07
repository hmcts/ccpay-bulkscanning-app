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
public class ReportDataUnprocessed extends BaseReportData {

    private String respServiceId;
    private String respServiceName;
    private String exceptionRef;
    private String ccdRef;
    private String dateBanked;
    private String bgcBatch;
    private String paymentAssetDcn;
    private String paymentMethod;
    private BigDecimal amount;
}
