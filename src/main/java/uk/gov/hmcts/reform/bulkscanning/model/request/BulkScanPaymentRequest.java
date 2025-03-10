package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createBSPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class BulkScanPaymentRequest {

    @JsonProperty("site_id")
    @NotBlank(message = "site_id can't be Blank")
    @Size(min = 4, max = 4, message = "site_id length must be 4 Characters")
    private String responsibleServiceId;

    @JsonProperty("ccd_case_number")
    @NotBlank(message = "ccd_case_number can't be Blank")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "ccd_case_number should be numeric")
    @Size(min = 16, max = 16, message = "ccd_case_number length must be 16 digits")
    private String ccdCaseNumber;

    @JsonProperty("is_exception_record")
    @NotNull(message = "is_exception_record flag can't be Blank")
    private Boolean isExceptionRecord;

    @JsonProperty("document_control_numbers")
    @NotEmpty(message = "document_control_numbers can't be Blank")
    private String[] documentControlNumbers;

    @JsonIgnore
    @AssertFalse(message = "Invalid site_id. Accepted values are AA08, AA07, AA09, ABA1 and ABA5")
    public boolean isValidResponsibleServiceId() {
        String[] validResponsibleServiceIds = {"AA08", "AA07", "AA09", "ABA1", "ABA5"};
        return responsibleServiceId != null && !Arrays.asList(validResponsibleServiceIds).stream().anyMatch(vm -> vm.equalsIgnoreCase(
            responsibleServiceId));
    }

    @JsonIgnore
    @AssertFalse(message = "document_control_number must be 21 digit numeric")
    public boolean isValidDocumentControlNumbers() {
        return documentControlNumbers != null
            && (Arrays.asList(documentControlNumbers).stream().anyMatch(dcn -> dcn.length() != 21)
            || Arrays.asList(documentControlNumbers).stream().anyMatch(dcn -> ! dcn.matches("-?\\d+(\\.\\d+)?")));
    }
}
