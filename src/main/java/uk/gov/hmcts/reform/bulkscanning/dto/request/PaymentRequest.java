package uk.gov.hmcts.reform.bulkscanning.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentRequest {

    /*
    Document control number of the payment received in the envelope.
    This is the unique identifier of the payment
     */
    @NotNull(message = "document_control_number can't be Empty")
    @JsonProperty("document_control_number")
    private String documentControlNumber;

    /*
    Payment amount in GBP
     */
    @NotNull(message = "amount can't be Empty")
    private BigDecimal amount;

    /*
    The ISO currency code
     */
    @NotNull(message = "currency can't be Empty")
    private String currency;

    /*
    The method of payment i.e. Cheque or Postal Order
     */
    //@NotEmpty(message = "paymentMethod can't be Empty")
    private String method;

    /*
    Number of the credit slip containing the payment
     */
    @NotNull(message = "bank_giro_credit_slip_number can't be Empty")
    @JsonProperty("bank_giro_credit_slip_number")
    private String bankGiroCreditSlipNumber;

    /*
    Date the payment was sent for banking.
     */
    @NotNull(message = "banked_date can't be Null")
    private Date bankedDate;
}
