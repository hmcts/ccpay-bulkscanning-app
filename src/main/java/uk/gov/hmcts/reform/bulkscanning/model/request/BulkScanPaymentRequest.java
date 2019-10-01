package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createBSPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class BulkScanPaymentRequest {

    @JsonProperty("site_id")
    private ResponsibleSiteId responsibleServiceId;

    @JsonProperty("ccd_case_number")
    @NotBlank(message = "CCD reference is missing")
    private String ccdCaseNumber;

    @JsonProperty("is_exception_record")
    @NotNull(message = "Exception record flag is missing")
    private Boolean isExceptionRecord;

    @JsonProperty("document_control_numbers")
    @NotEmpty(message = "Payment DCN are missing")
    private String[] documentControlNumbers;
}
