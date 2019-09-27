package uk.gov.hmcts.reform.bulkscanning.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "recordWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReportData {

    private String exceptionRef;
    private String ccdRef;
    private String respServiceId;
    private String respServiceName;
    private String dateBanked;
    private String bgcBatch;
    private String paymentMethod;
    private String paymentAssetDcn;
    private BigDecimal amount;
    private String lossResp;
}
