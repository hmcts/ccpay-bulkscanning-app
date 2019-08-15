package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.utils.DateUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.dateToLocalDateTime;

@Component
public class PaymentMetadataDTOMapper {

    public PaymentMetadata toPaymentEntity(PaymentMetadataDTO paymentMetadataDto){
        return PaymentMetadata.paymentMetadataWith()
            .bgcReference(paymentMetadataDto.getBgcReference())
            .dcnReference(paymentMetadataDto.getDcnReference())
            .paymentMethod(paymentMetadataDto.getPaymentMethod().toString())
            .amount(paymentMetadataDto.getAmount())
            .currency(paymentMetadataDto.getCurrency().toString())
            .dateBanked(dateToLocalDateTime(paymentMetadataDto.getDateBanked()))
            .build();
    }

    public PaymentMetadataDTO fromRequest(PaymentRequest paymentRequest){
        return PaymentMetadataDTO.paymentMetadataDtoWith()
            .dcnReference(paymentRequest.getDocument_control_number())
            .bgcReference(paymentRequest.getBank_giro_credit_slip_number())
            .amount(paymentRequest.getAmount())
            .currency(Currency.valueOf(paymentRequest.getCurrency()))
            .paymentMethod(PaymentMethod.valueOf(paymentRequest.getMethod()))
            .dateBanked(paymentRequest.getBanked_date())
            .build();
    }

}

