package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.request.ExelaPaymentRequest;

import java.util.Optional;

@Component
public class PaymentDtoMapper {

    public PaymentDto fromRequest(ExelaPaymentRequest exelaPaymentRequest, String dcnReference) {
        if(Optional.ofNullable(exelaPaymentRequest).isPresent()) {
            return PaymentDto.paymentDtoWith()
                .dcnReference(dcnReference)
                .paymentStatus(PaymentStatus.INCOMPLETE)
                .build();
        }else {
            return null;
        }
    }
}
