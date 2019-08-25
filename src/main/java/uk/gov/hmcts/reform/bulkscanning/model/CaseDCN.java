package uk.gov.hmcts.reform.bulkscanning.model;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
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

    @JoinColumn (name = "dcn_payment")
    @OneToOne(fetch = FetchType.EAGER)
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
