package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentMetadataDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.Currency;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentMetadataDtoMapperTest {

    PaymentMetadataDtoMapper paymentMetadataDtoMapper = new PaymentMetadataDtoMapper();

    public static LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? LocalDateTime.ofInstant(
            new java.util.Date(0).toInstant(),
            ZoneId.systemDefault()
        ) : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    @Test
    public void testToPaymentEntity() throws ParseException {
        SimpleDateFormat textFormat = new SimpleDateFormat("dd-MM-yyyy");

        PaymentMetadataDto paymentMetadataDto = PaymentMetadataDto.paymentMetadataDtoWith()
                                                    .bgcReference("bgc-reference")
                                                    .dcnReference("dcn-reference")
                                                    .paymentMethod(PaymentMethod.CASH)
                                                    .amount(BigDecimal.valueOf(100.00))
                                                    .currency(Currency.GBP)
                                                    .dateBanked(textFormat.parse("01-01-2020"))
                                                    .build();
        PaymentMetadata expectedPaymentMetaData = PaymentMetadata.paymentMetadataWith()
                                                    .amount(BigDecimal.valueOf(100))
                                                    .bgcReference("bgc-reference")
                                                    .dcnReference("dcn-reference")
                                                    .currency(Currency.GBP.name())
                                                    .paymentMethod(PaymentMethod.CASH.toString())
                                                    .amount(BigDecimal.valueOf(100.00))
                                                    .dateBanked(dateToLocalDateTime(textFormat.parse("01-01-2020")))
                                                    .build();
        PaymentMetadata actualPaymentMetaData = paymentMetadataDtoMapper.toPaymentEntity(paymentMetadataDto);
        assertThat(expectedPaymentMetaData).isEqualToComparingFieldByField(actualPaymentMetaData);
    }

    @Test
    public void testFromRequest() throws ParseException {
        SimpleDateFormat textFormat = new SimpleDateFormat("yyyy-MM-dd");

        PaymentMetadataDto expectedPaymentMetadataDto = PaymentMetadataDto.paymentMetadataDtoWith()
            .bgcReference("2134")
            .dcnReference("dcn-reference")
            .paymentMethod(PaymentMethod.CASH)
            .amount(BigDecimal.valueOf(100))
            .currency(Currency.GBP)
            .dateBanked(textFormat.parse("2020-01-01"))
            .build();

        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                                .method("CASH")
                                                .bankedDate("2020-01-01")
                                                .amount(BigDecimal.valueOf(100)).currency("GBP").bankGiroCreditSlipNumber(2134).build();
        PaymentMetadataDto actualPaymentMetaData = paymentMetadataDtoMapper.fromRequest(bulkScanPayment,"dcn-reference");
        assertThat(expectedPaymentMetadataDto).isEqualToComparingFieldByField(actualPaymentMetaData);
    }

    @Test
    public void testFromEntity() throws ParseException {
        SimpleDateFormat textFormat = new SimpleDateFormat("dd-MM-yyyy");
        PaymentMetadata requestPaymentMetaData = PaymentMetadata.paymentMetadataWith()
            .amount(BigDecimal.valueOf(100))
            .bgcReference("bgc-reference")
            .dcnReference("dcn-reference")
            .currency(Currency.GBP.name())
            .paymentMethod(PaymentMethod.CASH.toString())
            .amount(BigDecimal.valueOf(100.00))
            .dateCreated(dateToLocalDateTime(textFormat.parse("01-01-2020")))
            .dateUpdated(dateToLocalDateTime(textFormat.parse("02-01-2020")))
            .dateBanked(dateToLocalDateTime(textFormat.parse("05-01-2020")))
            .build();
        PaymentMetadataDto expectedPaymentMetadataDto = PaymentMetadataDto.paymentMetadataDtoWith()
            .bgcReference("bgc-reference")
            .dcnReference("dcn-reference")
            .paymentMethod(PaymentMethod.CASH)
            .amount(BigDecimal.valueOf(100.00))
            .currency(Currency.GBP)
            .dateCreated(textFormat.parse("01-01-2020"))
            .dateUpdated(textFormat.parse("02-01-2020"))
            .dateBanked(textFormat.parse("05-01-2020"))
            .build();

        PaymentMetadataDto actualPaymentMetaDataDto = paymentMetadataDtoMapper.fromEntity(requestPaymentMetaData);
        assertThat(expectedPaymentMetadataDto).isEqualToComparingFieldByField(actualPaymentMetaDataDto);
    }

}
