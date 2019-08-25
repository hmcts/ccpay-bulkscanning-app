package uk.gov.hmcts.reform.bulkscanning.dto;

import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_EMPTY)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Wither

public class CaseDCNDto {

    private String dcnPayment;
    private String ccdCaseNumber;
    private String exceptionReference;
    private String siteId;

    private PaymentDto payment;

    private Date dateCreated;

    private Date dateUpdated;

    public CaseDCNDto caseNumber(String ccdCaseNumber, Boolean isExceptionRecord) {

        if (isExceptionRecord) {
            this.exceptionReference = ccdCaseNumber;
        } else {
            this.ccdCaseNumber = ccdCaseNumber;
        }
        return this;
    }
}
