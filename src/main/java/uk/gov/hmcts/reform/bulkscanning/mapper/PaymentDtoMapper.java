package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;

import java.util.Optional;

@Component
public class PaymentDtoMapper {

    public PaymentDto fromRequest(BulkScanPayment bulkScanPayment, String dcnReference) {
        if(Optional.ofNullable(bulkScanPayment).isPresent()) {
            return PaymentDto.paymentDtoWith()
                .dcnReference(dcnReference)
                .paymentStatus(PaymentStatus.INCOMPLETE)
                .build();
        }else {
            return null;
        }
    }
}
