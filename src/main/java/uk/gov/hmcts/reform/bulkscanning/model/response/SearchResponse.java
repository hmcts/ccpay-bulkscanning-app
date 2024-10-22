package uk.gov.hmcts.reform.bulkscanning.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Builder(builderMethodName = "searchResponseWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchResponse implements Serializable {
    private String ccdReference;
    private String exceptionRecordReference;
    private String responsibleServiceId;
    private List<PaymentMetadataDto> payments;
    private PaymentStatus allPaymentsStatus;
}
