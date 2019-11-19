package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "searchRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchRequest {

    @NotNull(message = "ccd_reference can't be Blank")
    @Size(min = 16, max = 16, message = "ccd_reference length must be 16 Characters")
    private String ccdReference;

    @NotNull(message = "exception_record can't be Blank")
    @Size(min = 16, max = 16, message = "exception_record length must be 16 Characters")
    private String exceptionRecord;

    @NotNull(message = "document_control_number can't be Blank")
    @Size(min = 17, max = 17, message = "document_control_number length must be 17 Characters")
    private String documentControlNumber;
}
