package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod.POSTAL_ORDER;
import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.dateToLocalDateTime;
import static uk.gov.hmcts.reform.bulkscanning.utils.DateUtil.localDateTimeToDate;

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
                .build();
        }else {
            return null;
        }
    }

    public PaymentMetadataDto fromRequest(BulkScanPayment bulkScanPayment, String dcnReference) {
        if(Optional.ofNullable(bulkScanPayment).isPresent()) {
            String paymentMethod = bulkScanPayment.getMethod().equalsIgnoreCase("PostalOrder")
                ? POSTAL_ORDER.toString()
                : bulkScanPayment.getMethod().toUpperCase(Locale.UK);
            return PaymentMetadataDto.paymentMetadataDtoWith()
                .dcnReference(dcnReference)
                .bgcReference(bulkScanPayment.getBankGiroCreditSlipNumber().toString())
                .amount(bulkScanPayment.getAmount())
                .currency(Currency.valueOf(bulkScanPayment.getCurrency().toUpperCase(Locale.UK)))
                .paymentMethod(PaymentMethod.valueOf(paymentMethod))
                .dateBanked(DateTime.parse(bulkScanPayment.getBankedDate()).withZone(DateTimeZone.UTC).toDate())
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

