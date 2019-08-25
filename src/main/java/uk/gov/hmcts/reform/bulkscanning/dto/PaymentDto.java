package uk.gov.hmcts.reform.bulkscanning.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Positive;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;
import uk.gov.hmcts.reform.bulkscanning.model.PaymentMethod;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_EMPTY)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Wither
@SuppressWarnings("PMD")
public class PaymentDto {

    private String dcnPayment;

    @DecimalMin("0.01")
    @Positive
    @Digits(integer = 10, fraction = 2, message = "PaymentDto amount cannot have more than 2 decimal places")
    private BigDecimal amount;
    private String currency;
    private PaymentMethod method;
    private String bankGiroCreditSlipNumber;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate bankedDate;
    private String outboundBatchNumber;
    private String dcnCase;
    private String caseReference;

    private String poBox;
    private String firstChequeDCNInBatch;
    private String payerName;

    @JsonIgnore
    private Date dateCreated;

    @JsonIgnore
    private Date dateUpdated;

}
