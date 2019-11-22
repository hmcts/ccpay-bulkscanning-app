package uk.gov.hmcts.reform.bulkscanning.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.Currency.GBP;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod.CHEQUE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public class SearchServiceTest {
    MockMvc mockMvc;

    private SearchService paymentService;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private PaymentMetadataRepository paymentMetadataRepository;

    @MockBean
    private EnvelopeRepository envelopeRepository;

    @MockBean
    private EnvelopeCaseRepository envelopeCaseRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PaymentMetadataDtoMapper paymentMetadataDtoMapper;

    public static final String CCD_CASE_REFERENCE = "11112222333344441";
    public static final String TEST_DCN_REFERENCE = "123-123";

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        paymentService = new SearchServiceImpl(paymentRepository,
                                                paymentMetadataRepository,
                                                paymentMetadataDtoMapper,
                                                envelopeCaseRepository);
        Optional<PaymentMetadata> paymentMetadata = Optional.of(PaymentMetadata.paymentMetadataWith()
            .id(1).amount(BigDecimal.valueOf(100))
            .dcnReference(TEST_DCN_REFERENCE)
            .dateBanked(LocalDateTime.now())
            .paymentMethod(CHEQUE.toString()).currency(GBP.toString())
            .build());
        when(paymentMetadataRepository.save(any(PaymentMetadata.class)))
            .thenReturn(paymentMetadata.get());

        Optional<EnvelopePayment> envelopePayment = Optional.of(EnvelopePayment.paymentWith()
                                                                    .id(1)
                                                                    .dcnReference(TEST_DCN_REFERENCE)
                                                                    .paymentStatus(COMPLETE.toString())
                                                                    .build());
        Optional<List<EnvelopePayment>> payments = Optional.of(Arrays.asList(envelopePayment.get()));
        Optional<Envelope> envelope = Optional.of(Envelope.envelopeWith().id(1).envelopePayments(payments.get())
                                                      .paymentStatus(COMPLETE.toString())
                                                      .build());
        Optional<EnvelopeCase> envelopeCase = Optional.of(EnvelopeCase.caseWith()
                                                              .id(1)
                                                              .envelope(envelope.get())
                                                              .ccdReference(TEST_DCN_REFERENCE)
                                                              .exceptionRecordReference("ex123-123")
                                                              .build());
        Optional<List<EnvelopeCase>> cases = Optional.of(Arrays.asList(envelopeCase.get()));
        envelopePayment.get().setEnvelope(envelope.get());
        doReturn(envelopePayment).when(paymentRepository).findByDcnReference(TEST_DCN_REFERENCE);
        when(paymentRepository.save(any(EnvelopePayment.class))).thenReturn(envelopePayment.get());
        when(paymentRepository.findByEnvelopeId(any(Integer.class))).thenReturn(payments);
        when(envelopeRepository.save(any(Envelope.class))).thenReturn(envelope.get());
        when(envelopeCaseRepository.findByCcdReference(any(String.class))).thenReturn(cases);
        when(envelopeCaseRepository.findByExceptionRecordReference(TEST_DCN_REFERENCE)).thenReturn(cases);
        when(envelopeCaseRepository.findByEnvelopeId(any(Integer.class))).thenReturn(envelopeCase);
        when(paymentMetadataRepository.findByDcnReference(TEST_DCN_REFERENCE)).thenReturn(paymentMetadata);
    }

    @Test
    @Transactional
    public void testRetrieveByCCDReference() throws Exception {
        SearchResponse searchResponse = paymentService.retrieveByCCDReference(TEST_DCN_REFERENCE);
        assertThat(searchResponse.getCcdReference()).isEqualTo(TEST_DCN_REFERENCE);
    }

    @Test
    @Transactional
    public void testRetrieveByExceptionRecord() throws Exception {
        when(envelopeCaseRepository.findByCcdReference("EXP123")).thenReturn(Optional.empty());
        Optional<EnvelopePayment> envelopePayment = Optional.of(EnvelopePayment.paymentWith()
                                                                    .id(1)
                                                                    .dcnReference(TEST_DCN_REFERENCE)
                                                                    .paymentStatus(COMPLETE.toString())
                                                                    .build());
        Optional<List<EnvelopePayment>> payments = Optional.of(Arrays.asList(envelopePayment.get()));
        Optional<Envelope> envelope = Optional.of(Envelope.envelopeWith().id(1).envelopePayments(payments.get())
                                                      .paymentStatus(COMPLETE.toString())
                                                      .build());
        Optional<EnvelopeCase> envelopeCase = Optional.of(EnvelopeCase.caseWith()
                                                              .id(1)
                                                              .envelope(envelope.get())
                                                              .ccdReference("CCD123")
                                                              .exceptionRecordReference("EXP123")
                                                              .build());
        Optional<List<EnvelopeCase>> cases = Optional.of(Arrays.asList(envelopeCase.get()));
        when(envelopeCaseRepository.findByExceptionRecordReference("EXP123")).thenReturn(cases);
        when(envelopeCaseRepository.findByCcdReference("CCD123")).thenReturn(cases);
        SearchResponse searchResponse = paymentService.retrieveByCCDReference("EXP123");
        assertThat(searchResponse.getCcdReference()).isEqualTo("CCD123");
    }

    @Test
    @Transactional
    public void testRetrieveByDcn() throws Exception {
        SearchResponse searchResponse = paymentService.retrieveByDcn(TEST_DCN_REFERENCE);
        assertThat(searchResponse.getPayments().get(0).getDcnReference()).isEqualTo(TEST_DCN_REFERENCE);
    }
}
