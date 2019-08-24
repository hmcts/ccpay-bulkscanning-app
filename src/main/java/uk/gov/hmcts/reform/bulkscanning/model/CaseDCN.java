package uk.gov.hmcts.reform.bulkscanning.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_EMPTY)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Wither

public class CaseDCN {

    @Id
    private String dcnPayment;
    private String ccdCaseNumber;
    private String exceptionReference;
    private String siteId;

    @OneToOne
    private Payment payment;

    @JsonIgnore
    @CreationTimestamp
    private Date dateCreated;

    @JsonIgnore
    @UpdateTimestamp
    private Date dateUpdated;

    public CaseDCN caseNumber(String ccdCaseNumber, Boolean isExceptionRecord) {

        if (isExceptionRecord) {
            this.exceptionReference = ccdCaseNumber;
        } else {
            this.ccdCaseNumber = ccdCaseNumber;
        }
        return this;
    }
}
