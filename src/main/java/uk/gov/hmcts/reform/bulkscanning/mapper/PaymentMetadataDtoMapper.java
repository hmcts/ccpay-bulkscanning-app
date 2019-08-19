package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;

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
            .outboundBatchNumber(paymentMetadataDto.getOutboundBatchNumber())
            .dcnCase(paymentMetadataDto.getDcnCase())
            .caseReference(paymentMetadataDto.getCaseReference())
            .poBox(paymentMetadataDto.getPoBox())
            .firstChequeDcnInBatch(paymentMetadataDto.getFirstChequeDcnInBatch())
            .payerName(paymentMetadataDto.getPayerName())
            .build();
    }

    public PaymentMetadataDto fromRequest(PaymentRequest paymentRequest, String dcnReference) {
        return PaymentMetadataDto.paymentMetadataDtoWith()
            .dcnReference(dcnReference)
            .bgcReference(paymentRequest.getBankGiroCreditSlipNumber())
            .amount(paymentRequest.getAmount())
            .currency(Currency.valueOf(paymentRequest.getCurrency()))
            .paymentMethod(PaymentMethod.valueOf(paymentRequest.getMethod()))
            .dateBanked(paymentRequest.getBankedDate())
            .outboundBatchNumber(paymentRequest.getOutboundBatchNumber())
            .dcnCase(paymentRequest.getDcnCase())
            .caseReference(paymentRequest.getCaseReference())
            .poBox(paymentRequest.getPoBox())
            .firstChequeDcnInBatch(paymentRequest.getFirstChequeDcnInBatch())
            .payerName(paymentRequest.getPayerName())
            .build();
    }

}

