package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PaymentDtoMapperTest {

    PaymentDtoMapper paymentDtoMapper = new PaymentDtoMapper();

    @Test
    public void testFromRequest(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .build();
        PaymentDto paymentDto = paymentDtoMapper.fromRequest(bulkScanPayment,"dcn-reference");
        assertEquals("Exela",paymentDto.getSource());
    }

    @Test
    public void  testFromRequest_WithNullPayment(){
        PaymentDto paymentDto = paymentDtoMapper.fromRequest(null,"dcn-reference");
        assertNull(paymentDto);
    }
}
