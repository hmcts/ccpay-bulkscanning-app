package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @JsonProperty("responsible_service_id")
    @NotNull(message = "Responsible service id is missing")
    private String responsibleServiceId;

    @JsonProperty("ccd_case_number")
    @NotNull(message = "Case number is missing")
    private String ccdCaseNumber;

    @JsonProperty("is_exception_record")
    @NotNull(message = "Exception record flag is missing")
    private Boolean isExceptionRecord;

    @JsonProperty("document_control_numbers")
    @NotEmpty(message = "Document Control Numbers are missing")
    private String[] documentControlNumbers;
}
