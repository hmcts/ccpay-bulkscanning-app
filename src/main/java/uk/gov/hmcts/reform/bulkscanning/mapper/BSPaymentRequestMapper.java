package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BSPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class BSPaymentRequestMapper {

     public Payment mapPaymentFromBSPaymentRequest(BSPaymentRequest bsPaymentRequest) {
         return Payment.paymentWith()
             .dcnReference(bsPaymentRequest.getDocumentControlNumber())
             .amount(bsPaymentRequest.getAmount())
             .currency(Currency.valueOf(bsPaymentRequest.getCurrency()))
             .paymentMethod(PaymentMethod.valueOf(bsPaymentRequest.getMethod()))
             .bgcReference(bsPaymentRequest.getBankGiroCreditSlipNumber())
             .dateBanked(bsPaymentRequest.getBankedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
             .dateCreated(LocalDateTime.now())
             .dateUpdated(LocalDateTime.now())
             .build();
     }
}
