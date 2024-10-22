package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "searchRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchRequest {

    @NotNull(message = "ccd_reference can't be Blank")
    @Size(min = 16, max = 16, message = "ccd_reference length must be 16 Characters")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "ccd_reference should be numeric")
    private String ccdReference;

    @NotNull(message = "exception_record can't be Blank")
    @Size(min = 16, max = 16, message = "exception_record length must be 16 Characters")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "exception_record should be numeric")
    private String exceptionRecord;

    @NotNull(message = "document_control_number can't be Blank")
    @Size(min = 21, max = 21, message = "document_control_number length must be 21 Characters")
    @Pattern(regexp="-?\\d+(\\.\\d+)?", message = "document_control_number should be numeric")
    private String documentControlNumber;
}
