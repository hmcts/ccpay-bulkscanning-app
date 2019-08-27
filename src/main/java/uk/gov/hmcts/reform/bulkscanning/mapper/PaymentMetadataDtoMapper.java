package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.request.PaymentRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.*;

@Component
public class PaymentMetadataDtoMapper {

    public PaymentMetadata toPaymentEntity(PaymentMetadataDto paymentMetadataDto) {
        if(Optional.ofNullable(paymentMetadataDto).isPresent()) {
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
        }else {
            return null;
        }

    }

    public PaymentMetadataDto fromRequest(PaymentRequest paymentRequest, String dcnReference) {
        if(Optional.ofNullable(paymentRequest).isPresent()) {
            return PaymentMetadataDto.paymentMetadataDtoWith()
                .dcnReference(dcnReference)
                .bgcReference(paymentRequest.getBankGiroCreditSlipNumber())
                .amount(paymentRequest.getAmount())
                .currency(Currency.valueOf(paymentRequest.getCurrency()))
                .paymentMethod(PaymentMethod.valueOf(paymentRequest.getMethod()))
                .dateBanked(localDateTimeToDate(paymentRequest.getBankedDate()))
                .outboundBatchNumber(paymentRequest.getOutboundBatchNumber())
                .dcnCase(paymentRequest.getDcnCase())
                .caseReference(paymentRequest.getCaseReference())
                .poBox(paymentRequest.getPoBox())
                .firstChequeDcnInBatch(paymentRequest.getFirstChequeDcnInBatch())
                .payerName(paymentRequest.getPayerName())
                .build();
        }else {
            return null;
        }
    }

    public PaymentMetadataDto fromEntity(PaymentMetadata paymentMetadata) {
        if (Optional.ofNullable(paymentMetadata).isPresent()) {
            return PaymentMetadataDto.paymentMetadataDtoWith()
                .id(paymentMetadata.getId())
                .dcnReference(paymentMetadata.getDcnReference())
                .bgcReference(paymentMetadata.getBgcReference())
                .amount(paymentMetadata.getAmount())
                .currency(Currency.valueOf(paymentMetadata.getCurrency()))
                .paymentMethod(PaymentMethod.valueOf(paymentMetadata.getPaymentMethod()))
                .dateBanked(localDateTimeToDate(paymentMetadata.getDateBanked()))
                .outboundBatchNumber(paymentMetadata.getOutboundBatchNumber())
                .dcnCase(paymentMetadata.getDcnCase())
                .caseReference(paymentMetadata.getCaseReference())
                .poBox(paymentMetadata.getPoBox())
                .firstChequeDcnInBatch(paymentMetadata.getFirstChequeDcnInBatch())
                .payerName(paymentMetadata.getPayerName())
                .dateCreated(localDateTimeToDate(paymentMetadata.getDateCreated()))
                .dateUpdated(localDateTimeToDate(paymentMetadata.getDateUpdated()))
                .build();
        }
        return null;
    }

    public List<PaymentMetadataDto> fromPaymentMetadataEntities(List<PaymentMetadata> metadataList) {
        return metadataList.stream().map(this::fromEntity).collect(Collectors.toList());
    }

}

