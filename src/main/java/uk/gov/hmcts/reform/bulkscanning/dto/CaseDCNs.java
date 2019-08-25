package uk.gov.hmcts.reform.bulkscanning.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCN;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Wither
@Builder
public class CaseDCNs {

    @NotBlank
    private String ccdCaseNumber;
    @NotBlank
    private Boolean isExceptionRecord;
    @NotBlank
    private String siteId;
    @NotBlank
    private String[] documentControlNumbers;

    @JsonIgnore
    public List<CaseDCN> getCasePayments() {

        return Arrays.stream(documentControlNumbers).map(dcn ->
            CaseDCN.builder().dcnPayment(dcn).siteId(siteId).build().caseNumber(ccdCaseNumber, isExceptionRecord))
            .collect(Collectors.toList());
    }
}
