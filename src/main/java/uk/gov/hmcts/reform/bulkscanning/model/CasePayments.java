package uk.gov.hmcts.reform.bulkscanning.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@JsonInclude(NON_NULL)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Wither
@Builder
public class CasePayments implements Serializable {

    private String ccdCaseNumber;
    private Boolean isExceptionRecord;
    private String siteId;
    private String[] documentControlNumbers;

    public List<Payment> getPayments() {

        return Arrays.stream(documentControlNumbers).map(dcn ->
            Payment.builder().dcnPayment(dcn).siteId(siteId).build().ccdCaseNumber(ccdCaseNumber, isExceptionRecord))
            .collect(Collectors.toList());
    }
}
