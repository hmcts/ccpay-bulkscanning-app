package uk.gov.hmcts.reform.bulkscanning.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
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
    //@NotEmpty(message = "document_control_number can't be Empty")
    private String document_control_number;

    /*
    Payment amount in GBP
     */
    //@NotEmpty(message = "amount can't be Empty")
    private BigDecimal amount;

    /*
    The ISO currency code
     */
    //@NotEmpty(message = "currency can't be Empty")
    private String currency;

    /*
    The method of payment i.e. Cheque or Postal Order
     */
    //@NotEmpty(message = "paymentMethod can't be Empty")
    private String paymentMethod;

    /*
    Number of the credit slip containing the payment
     */
    //@NotEmpty(message = "bank_giro_credit_slip_number can't be Empty")
    private String bank_giro_credit_slip_number;

    /*
    Date the payment was sent for banking.
     */
    //@NotEmpty(message = "banked_date can't be Empty")
    private Date banked_date;
}
