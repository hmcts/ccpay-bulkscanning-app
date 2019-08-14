package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDTO;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class PaymentMetadataDTOMapper {

    public PaymentMetadata toPaymentEntity(PaymentMetadataDTO paymentMetadataDto){
        return PaymentMetadata.paymentMetadataWith()
            .id(paymentMetadataDto.getId())
            .bgcReference(paymentMetadataDto.getBgcReference())
            .dcnReference(paymentMetadataDto.getDcnReference())
            .paymentMethod(paymentMetadataDto.getPaymentMethod())
            .amount(paymentMetadataDto.getAmount())
            .currency(paymentMetadataDto.getCurrency())
            .dateBanked(LocalDateTime.ofInstant(paymentMetadataDto.getDateBanked().toInstant(), ZoneId.systemDefault()))
            .dateCreated(LocalDateTime.ofInstant(paymentMetadataDto.getDateCreated().toInstant(), ZoneId.systemDefault()))
            .build();
    }

    public PaymentMetadataDTO fromRequest(PaymentRequest paymentRequest){
        return PaymentMetadataDTO.paymentMetadataDtoWith()
            .dcnReference(paymentRequest.getDocument_control_number())
            .bgcReference(paymentRequest.getBank_giro_credit_slip_number())
            .amount(paymentRequest.getAmount())
            .currency(Currency.valueOf(paymentRequest.getCurrency()))
            .dateBanked(paymentRequest.getBanked_date())
            .build();
    }

}

