package uk.gov.hmcts.reform.bulkscanning.utils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelope;
import static uk.gov.hmcts.reform.bulkscanning.service.PaymentServiceTest.CCD_CASE_REFERENCE;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
//@TestPropertySource(locations="classpath:application-local.yaml")
public class BulkScanningUtilsTest {

    @Autowired
    private BulkScanningUtils bulkScanningUtils;

    @Autowired
    private EnvelopeRepository envelopeRepository;

    @Test
    public void testInsertStatusHistoryTest() {
        Envelope bsEnvelope =  Envelope.envelopeWith().dateCreated(LocalDateTime.now()).build();

        EnvelopePayment payment1 = EnvelopePayment.paymentWith()
            .dcnReference("888888888888888888888")
            .envelope(bsEnvelope)
            .paymentStatus(PaymentStatus.INCOMPLETE.toString())
            .dateCreated(LocalDateTime.now()).build();

        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();
        envelopePaymentList.add(payment1);

        EnvelopeCase envelopeCase = EnvelopeCase.caseWith().id(1).ccdReference(CCD_CASE_REFERENCE).envelope(bsEnvelope).dateCreated(LocalDateTime.now()).build();

        List<EnvelopeCase> envelopeCasesList = new ArrayList<>();
        envelopeCasesList.add(envelopeCase);

        bsEnvelope.setEnvelopePayments(envelopePaymentList);
        bsEnvelope.setEnvelopeCases(envelopeCasesList);
        bsEnvelope.setDateCreated(LocalDateTime.now());

        Envelope envelope = bulkScanningUtils.insertStatusHistoryAudit(bsEnvelope);
        Assert.assertEquals(1, envelope.getEnvelopePayments().size());
    }
}
