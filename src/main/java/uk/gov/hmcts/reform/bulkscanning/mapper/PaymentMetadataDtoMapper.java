package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.dto.request.PaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.dateToLocalDateTime;

@Component
public class PaymentMetadataDtoMapper {

    public PaymentMetadata toPaymentEntity(PaymentMetadataDto paymentMetadataDto) {
        return PaymentMetadata.paymentMetadataWith()
            .bgcReference(paymentMetadataDto.getBgcReference())
            .dcnReference(paymentMetadataDto.getDcnReference())
            .paymentMethod(paymentMetadataDto.getPaymentMethod().toString())
            .amount(paymentMetadataDto.getAmount())
            .currency(paymentMetadataDto.getCurrency().toString())
            .dateBanked(dateToLocalDateTime(paymentMetadataDto.getDateBanked()))
            .build();
    }

    public PaymentMetadataDto fromRequest(PaymentRequest paymentRequest) {
        return PaymentMetadataDto.paymentMetadataDtoWith()
            .dcnReference(paymentRequest.getDocumentControlNumber())
            .bgcReference(paymentRequest.getBankGiroCreditSlipNumber())
            .amount(paymentRequest.getAmount())
            .currency(Currency.valueOf(paymentRequest.getCurrency()))
            .paymentMethod(PaymentMethod.valueOf(paymentRequest.getMethod()))
            .dateBanked(paymentRequest.getBankedDate())
            .build();
    }

}

