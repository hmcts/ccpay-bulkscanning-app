package uk.gov.hmcts.reform.bulkscanning.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonInclude(NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class PaymentRequest {

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

    @JsonProperty("outbound_batch_number")
    private String outboundBatchNumber;

    @JsonProperty("dcn_case")
    private String dcnCase;

    @JsonProperty("case_reference")
    private String caseReference;

    @JsonProperty("po_box")
    private String poBox;

    @JsonProperty("first_cheque_dcn_in_batch")
    private String firstChequeDcnInBatch;

    @JsonProperty("payer_name")
    private String payerName;
}
