package uk.gov.hmcts.reform.bulkscanning.model.request;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createCaseReferenceRequest")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class CaseReferenceRequest {
    @JsonProperty("ccd_case_number")
    @NotBlank(message = "ccd_case_number can't be Blank")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "ccd_case_number should be numeric")
    @Size(min = 16, max = 16, message = "ccd_case_number length must be 16 Characters")
    String ccdCaseNumber;
}
