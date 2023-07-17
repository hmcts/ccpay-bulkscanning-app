package uk.gov.hmcts.reform.bulkscanning.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.audit.AppInsightsAuditRepository;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelope;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelopeNoEnvelope;
import static uk.gov.hmcts.reform.bulkscanning.functionaltest.PaymentControllerFnTest.createBulkScanPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.Currency.GBP;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod.CHEQUE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"local", "test"})
public class PaymentFromBulkScanTest {
    private PaymentService paymentService;

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

    @Autowired
    private EnvelopeDtoMapper envelopeDtoMapper;

    @Autowired
    private PaymentDtoMapper paymentDtoMapper;

    @MockBean
    private BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @MockBean
    private BulkScanningUtils bulkScanningUtils;

    private CaseReferenceRequest caseReferenceRequest;

    @Autowired
    private AppInsightsAuditRepository auditRepository;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    public static final String CCD_CASE_REFERENCE = "1111222233334444";
    public static final String CCD_CASE_REFERENCE_NOT_PRESENT = "9999888833334444";
    public static final String EXCEPTION_RECORD_REFERENCE = "4444333322221111";
    public static final String DCN_REFERENCE = "DCN111111111111111111";

    public static final String TEST_DCN_REFERENCE = "123123111111111111111";

    @Before
    public void setUp() {
        paymentService = new PaymentServiceImpl(
            paymentRepository,
            paymentMetadataRepository,
            envelopeRepository,
            paymentMetadataDtoMapper,
            envelopeDtoMapper,
            paymentDtoMapper,
            bsPaymentRequestMapper,
            bulkScanningUtils,
            envelopeCaseRepository,
            auditRepository
        );
        Optional<PaymentMetadata> paymentMetadata = Optional.of(PaymentMetadata.paymentMetadataWith().id(1).amount(
            BigDecimal.valueOf(100)).dcnReference(TEST_DCN_REFERENCE).dateBanked(LocalDateTime.now()).paymentMethod(
            CHEQUE.toString()).currency(GBP.toString()).build());
        when(paymentMetadataRepository.save(any(PaymentMetadata.class))).thenReturn(paymentMetadata.get());

        Optional<EnvelopePayment> envelopePayment = Optional.of(EnvelopePayment.paymentWith().id(1).dcnReference(
            TEST_DCN_REFERENCE).paymentStatus(COMPLETE.toString()).build());
        Optional<List<EnvelopePayment>> payments = Optional.of(Arrays.asList(envelopePayment.get()));
        Optional<Envelope> envelope = Optional.of(Envelope.envelopeWith().id(1).envelopePayments(payments.get()).paymentStatus(
            COMPLETE.toString()).build());
        Optional<EnvelopeCase> envelopeCase = Optional.of(EnvelopeCase.caseWith().id(1).envelope(envelope.get()).ccdReference(
            TEST_DCN_REFERENCE).exceptionRecordReference("ex123-123").build());
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

        caseReferenceRequest = CaseReferenceRequest.createCaseReferenceRequest().ccdCaseNumber(CCD_CASE_REFERENCE).build();
    }


    @Test
    @Transactional
    public void testProcessPaymentFromBulkScanNullListOfAllPayments() {
        String[] dcn = {"DCN1"};

        doReturn(null).when(bulkScanningUtils).returnExistingEnvelopeList(any());
        doReturn(Optional.ofNullable(null)).when(envelopeRepository).findById(null);
        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(
            CCD_CASE_REFERENCE,
            dcn,
            "AA08",
            true
        );

        List<String> listDCN = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDCN.isEmpty());
    }



    @Test
    @Transactional
    public void testProcessPaymentFromBulkScanEmptyListOfAllPayments() {
        String[] dcn = {"DCN1"};

        doReturn(new ArrayList<>()).when(bulkScanningUtils).returnExistingEnvelopeList(any());
        doReturn(Optional.ofNullable(null)).when(envelopeRepository).findById(null);
        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(
            CCD_CASE_REFERENCE,
            dcn,
            "AA08",
            true
        );

        List<String> listDCN = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDCN.isEmpty());
    }

    @Test
    @Transactional
    public void testProcessPaymentFromBulkScan() {
        String[] dcn = {"DCN1"};
        doReturn(Optional.ofNullable(mockBulkScanningEnvelope())).when(envelopeRepository).findById(null);
        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(
            CCD_CASE_REFERENCE,
            dcn,
            "AA08",
            true
        );

        List<String> listDCN = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDCN.isEmpty());
    }

    @Test
    @Transactional
    public void testProcessExistingPaymentFromBulkScan() {
        String[] dcn = {"DCN1"};
        EnvelopePayment envelopePayment = mockBulkScanningEnvelope().getEnvelopePayments().get(0);

        //setting mockBulkScanningEnvelope
        envelopePayment.setEnvelope(mockBulkScanningEnvelope());
        doReturn(Optional.ofNullable(envelopePayment)).when(paymentRepository).findByDcnReference("DCN1");

        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(
            CCD_CASE_REFERENCE,
            dcn,
            "AA08",
            true
        );
        paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);
    }


    @Test
    @Transactional
    public void testProcessPaymentFromBulkScanWithNoEnvelope() {
        String[] dcn = {"DCN1"};

        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(CCD_CASE_REFERENCE
            ,dcn,"AA08", false);


        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();

        List<EnvelopeCase> envelopeCaseList = new ArrayList<>();

        Envelope envelope = getEnvelope(mockBulkScanPaymentRequest, envelopePaymentList, envelopeCaseList);

        List<Envelope> returnExistingEnvelopeList = new ArrayList<>();
        returnExistingEnvelopeList.add(envelope);

        doReturn(returnExistingEnvelopeList).when(bulkScanningUtils).returnExistingEnvelopeList(any());
        doReturn(envelope).when(bsPaymentRequestMapper).mapEnvelopeFromBulkScanPaymentRequest(any());

        doReturn(Optional.ofNullable(mockBulkScanningEnvelopeNoEnvelope())).when(envelopeRepository).findById(null);

        List<String> listDCN = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDCN.get(0).equalsIgnoreCase("dcn1"));
    }

    @Test
    @Transactional
    public void testProcessPaymentFromBulkScanWithNullEnvelope() {
        String[] dcn = {"DCN1"};

        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(CCD_CASE_REFERENCE
            ,dcn,"AA08", false);


        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();

        List<EnvelopeCase> envelopeCaseList = new ArrayList<>();

        Envelope envelope = getEnvelope(mockBulkScanPaymentRequest, null, envelopeCaseList);

        List<Envelope> returnExistingEnvelopeList = new ArrayList<>();
        returnExistingEnvelopeList.add(envelope);

        doReturn(returnExistingEnvelopeList).when(bulkScanningUtils).returnExistingEnvelopeList(any());
        doReturn(envelope).when(bsPaymentRequestMapper).mapEnvelopeFromBulkScanPaymentRequest(any());

        doReturn(Optional.ofNullable(mockBulkScanningEnvelopeNoEnvelope())).when(envelopeRepository).findById(null);

        List<String> listDCN = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDCN.get(0).equalsIgnoreCase("dcn1"));
    }

    private static Envelope getEnvelope(BulkScanPaymentRequest mockBulkScanPaymentRequest, List<EnvelopePayment> envelopePaymentList, List<EnvelopeCase> envelopeCaseList) {
        Envelope envelope = Envelope.envelopeWith()
            .responsibleServiceId(ResponsibleSiteId.valueOf(mockBulkScanPaymentRequest.getResponsibleServiceId().toUpperCase(
                Locale.UK)).toString())
            .envelopePayments(envelopePaymentList)
            .envelopeCases(envelopeCaseList)
            .paymentStatus(INCOMPLETE.toString()) ////by default at initial status
            .build();
        return envelope;
    }

}
