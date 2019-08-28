package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "searchRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchRequest {

    @NotNull(message = "ccd_reference can't be Empty")
    private String ccdReference;

    @NotNull(message = "exception_record can't be Empty")
    private String exceptionRecord;

    @NotNull(message = "document_control_number can't be Empty")
    private String documentControlNumber;
}
