package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Builder(builderMethodName = "createPaymentRequestWith")
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonInclude(NON_NULL)
public class BulkScanPayment {

    @NotBlank(message = "document_control_number can't be Blank")
    @JsonProperty("document_control_number")
    private String dcnReference;
    /*
    Payment amount in GBP
     */
    @NotNull(message = "amount can't be Blank")
    @DecimalMin("0.01")
    private BigDecimal amount;

    /*
    The ISO currency code
     */
    @NotBlank(message = "currency can't be Blank")
    private String currency;

    /*
    The method of payment i.e. Cheque, Postal Order, CASH
     */
    @NotBlank(message = "payment_Method can't be Blank")
    private String method;

    /*
    Number of the credit slip containing the payment
     */
    @NotNull(message = "bank_giro_credit_slip_number can't be Blank")
    @JsonProperty("bank_giro_credit_slip_number")
    private Integer bankGiroCreditSlipNumber;

    /*
    Date the payment was sent for banking.
     */
    @NotNull(message = "banked_date can't be Blank")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDateTime bankedDate;
}
