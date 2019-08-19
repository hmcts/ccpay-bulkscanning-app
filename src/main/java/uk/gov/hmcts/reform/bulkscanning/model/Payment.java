package uk.gov.hmcts.reform.bulkscanning.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

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
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@SelectBeforeUpdate

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Wither
public class Payment {

    @Id
    private String dcnPayment;
    @NotNull
    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "Payment amount cannot have more than 2 decimal places")
    private BigDecimal amount;
    private String currency = "GBP";
    private PaymentMethod method;
    private String bankGiroCreditSlipNumber;
    private LocalDate bankedDate;
    private String outboundBatchNumber;
    private String dcnCase;
    private String caseReference;
    private String ccdCaseNumber;
    private String exceptionReference;
    private String poBox;
    private String firstChequeDCNInBatch;
    private String payerName;
    private String siteId;

    @CreationTimestamp
    @JsonIgnore
    private Date dateCreated;

    @JsonIgnore
    @UpdateTimestamp
    private Date dateUpdated;

    public Payment ccdCaseNumber(String ccdCaseNumber, Boolean isExceptionRecord) {

        if (isExceptionRecord) {
            this.exceptionReference = ccdCaseNumber;
        } else {
            this.ccdCaseNumber = ccdCaseNumber;
        }
        return this;
    }
}
