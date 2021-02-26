package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.junit.Test;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EnvelopeDtoMapperTest {

    EnvelopeDtoMapper  envelopeDtoMapper = new EnvelopeDtoMapper();

    @Test
    public void testToEnvelopeEntity(){
        PaymentDto paymentDto = PaymentDto.paymentDtoWith()
                                    .id(1)
                                    .dcnReference("dcn-reference")
                                    .source("source")
                                    .paymentStatus(PaymentStatus.COMPLETE)
                                    .build();
        List<PaymentDto> paymentList = new ArrayList<>();
        paymentList.add(paymentDto);
        EnvelopeDto envelopeDto = EnvelopeDto.envelopeDtoWith()
                                    .paymentStatus(PaymentStatus.COMPLETE)
                                    .payments(paymentList)
                                    .build();
        Envelope envelope = envelopeDtoMapper.toEnvelopeEntity(envelopeDto);
        assertEquals("COMPLETE",envelope.getPaymentStatus(),"Status should be COMPLETE");
    }

    @Test
    public void testToEnvelopeEntity_WithNull(){
        Envelope envelope = envelopeDtoMapper.toEnvelopeEntity(null);
        assertNull(envelope,"Response should be NULL");
    }

    @Test
    public void testToPaymentEntities(){
        PaymentDto paymentDto = PaymentDto.paymentDtoWith()
            .id(1)
            .dcnReference("dcn-reference")
            .source("source")
            .paymentStatus(PaymentStatus.COMPLETE)
            .build();
        List<PaymentDto> paymentList = new ArrayList<>();
        paymentList.add(paymentDto);
        List<EnvelopePayment> envelopePaymentList = envelopeDtoMapper.toPaymentEntities(paymentList);
        assertEquals("dcn-reference",envelopePaymentList.get(0).getDcnReference(),"DCN reference is invalid");
    }

}
