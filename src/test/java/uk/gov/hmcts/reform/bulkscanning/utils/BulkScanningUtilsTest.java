package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
//@TestPropertySource(locations="classpath:application-local.yaml")
public class BulkScanningUtilsTest {

    @Autowired
    private BulkScanningUtils bulkScanningUtils;

    @Test
    public void testInsertStatusHistoryTest() {
        Envelope bsEnvelope =  Envelope.envelopeWith().dateCreated(LocalDateTime.now()).build();

        EnvelopePayment payment1 = EnvelopePayment.paymentWith()
            .dcnReference("888888888888888888888")
            .envelope(bsEnvelope)
            .paymentStatus(INCOMPLETE.toString())
            .dateCreated(LocalDateTime.now()).build();

        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();
        envelopePaymentList.add(payment1);

        bsEnvelope.setEnvelopePayments(envelopePaymentList);
        bsEnvelope.setDateCreated(LocalDateTime.now());

        Envelope envelope = bulkScanningUtils.insertStatusHistoryAudit(bsEnvelope);
        assertThat( envelope.getEnvelopePayments().size()).isEqualTo(1);
    }
}
