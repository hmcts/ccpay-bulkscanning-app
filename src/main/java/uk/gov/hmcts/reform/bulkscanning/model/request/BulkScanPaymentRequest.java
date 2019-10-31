package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createBSPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class BulkScanPaymentRequest {

    @JsonProperty("site_id")
    @NotBlank(message = "site_id is missing")
    @Size(min = 4, max = 4, message = "site_id length must be 4 Characters")
    private String responsibleServiceId;

    @JsonProperty("ccd_case_number")
    @NotBlank(message = "CCD reference is missing")
    @Size(min = 16, max = 16, message = "ccd_case_number length must be 16 Characters")
    private String ccdCaseNumber;

    @JsonProperty("is_exception_record")
    @NotNull(message = "Exception record flag is missing")
    private Boolean isExceptionRecord;

    @JsonProperty("document_control_numbers")
    @NotEmpty(message = "Payment DCN are missing")
    private String[] documentControlNumbers;

    @JsonIgnore
    @AssertFalse(message = "Invalid ResponsibleServiceId. Examples could be AA08/AA07")
    public boolean isValidResponsibleServiceId() {
        String[] validResponsibleServiceIds = {"AA08", "AA07"};
        if (responsibleServiceId != null) {
            if (! Arrays.asList(validResponsibleServiceIds).stream().anyMatch(vm -> vm.equalsIgnoreCase(responsibleServiceId))) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    @AssertFalse(message = "document_control_number length must be 17 Characters")
    public boolean isValidDocumentControlNumbers() {
        if (documentControlNumbers != null) {
            if (Arrays.asList(documentControlNumbers).stream().anyMatch(dcn -> dcn.length() != 17)) {
                return true;
            }
        }
        return false;
    }
}
