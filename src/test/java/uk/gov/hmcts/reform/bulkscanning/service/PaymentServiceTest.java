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
import uk.gov.hmcts.reform.bulkscanning.exception.BulkScanCaseAlreadyExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.DcnNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.EnvelopeDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.mapper.PaymentMetadataDtoMapper;
import uk.gov.hmcts.reform.bulkscanning.model.dto.EnvelopeDto;
import uk.gov.hmcts.reform.bulkscanning.model.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.mockBulkScanningEnvelope;
import static uk.gov.hmcts.reform.bulkscanning.functionaltest.PaymentControllerFnTest.createBulkScanPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.Currency.GBP;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentMethod.CHEQUE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.COMPLETE;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"local", "test"})
public class PaymentServiceTest {
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

    @Autowired
    private BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @Autowired
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
        paymentService = new PaymentServiceImpl(paymentRepository,
                                                paymentMetadataRepository,
                                                envelopeRepository,
                                                paymentMetadataDtoMapper,
                                                envelopeDtoMapper,
                                                paymentDtoMapper,
                                                bsPaymentRequestMapper,
                                                bulkScanningUtils,
                                                envelopeCaseRepository,
                                                auditRepository);
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

        caseReferenceRequest = CaseReferenceRequest.createCaseReferenceRequest()
            .ccdCaseNumber(CCD_CASE_REFERENCE)
            .build();
    }

    @Test
    public void testProcessPaymentFromExela() throws Exception {
        Optional<EnvelopePayment> envelopePayment = Optional.of(EnvelopePayment.paymentWith()
                                                                    .id(1)
                                                                    .dcnReference(TEST_DCN_REFERENCE)
                                                                    .paymentStatus(COMPLETE.toString())
                                                                    .build());
        Optional<List<EnvelopePayment>> payments = Optional.of(Arrays.asList(envelopePayment.get()));
        Optional<Envelope> envelope = Optional.of(Envelope.envelopeWith().id(1).envelopePayments(payments.get())
                                                      .paymentStatus(INCOMPLETE.toString())
                                                      .build());
        envelopePayment.get().setEnvelope(envelope.get());
        doReturn(envelopePayment).when(paymentRepository).findByDcnReference(any(String.class));

        Envelope envelopeMock = paymentService.processPaymentFromExela(createPaymentRequest(), TEST_DCN_REFERENCE);
        assertThat(envelopeMock.getEnvelopePayments().get(0).getDcnReference()).isEqualTo(TEST_DCN_REFERENCE);
    }

    @Test
    @Transactional
    public void testGetPaymentMetadata() throws Exception {
        PaymentMetadata paymentMetadata = paymentService.getPaymentMetadata(TEST_DCN_REFERENCE);
        assertThat(paymentMetadata.getDcnReference()).isEqualTo(TEST_DCN_REFERENCE);
    }

    private BulkScanPayment createPaymentRequest() {
        return BulkScanPayment.createPaymentRequestWith()
            .amount(BigDecimal.valueOf(100.00))
            .bankedDate("2019-10-31")
            .bankGiroCreditSlipNumber(123_456)
            .currency("GBP")
            .method("CHEQUE")
            .build();
    }


    //Bulk Scanning test cases

    @Test
    @Transactional
    public void testProcessPaymentFromBulkScan() throws Exception {
        String[] dcn = {"DCN1"};
        doReturn(Optional.ofNullable(mockBulkScanningEnvelope())).when(envelopeRepository).findById(null);
        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(CCD_CASE_REFERENCE,
                                                                                         dcn,"AA08", true);

        List<String> listDcn = paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);

        Assert.assertTrue(listDcn.get(0).equalsIgnoreCase("dcn1"));
    }

    @Test(expected = BulkScanCaseAlreadyExistsException.class)
    @Transactional
    public void testProcessExistingPaymentFromBulkScan() throws Exception {
        String[] dcn = {"DCN1"};
        EnvelopePayment envelopePayment = mockBulkScanningEnvelope().getEnvelopePayments().get(0);

        //setting mockBulkScanningEnvelope
        envelopePayment.setEnvelope(mockBulkScanningEnvelope());
        doReturn(Optional.ofNullable(envelopePayment)).when(paymentRepository).findByDcnReference("DCN1");

        BulkScanPaymentRequest mockBulkScanPaymentRequest = createBulkScanPaymentRequest(CCD_CASE_REFERENCE,dcn,"AA08", true);
        paymentService.saveInitialMetadataFromBs(mockBulkScanPaymentRequest);
    }

    @Test()
    @Transactional
    public void testUpdateCaseReferenceForExceptionRecord() throws Exception {
        List<EnvelopeCase> envelopeCaseList = new ArrayList<>();
        EnvelopeCase envelopeCase = EnvelopeCase.caseWith().exceptionRecordReference(EXCEPTION_RECORD_REFERENCE).id(1).build();
        envelopeCaseList.add(envelopeCase);

        doReturn(Optional.ofNullable(envelopeCaseList)).when(envelopeCaseRepository).findByExceptionRecordReference(EXCEPTION_RECORD_REFERENCE);

        Assert.assertTrue(paymentService
                              .updateCaseReferenceForExceptionRecord(EXCEPTION_RECORD_REFERENCE,caseReferenceRequest).equalsIgnoreCase("1"));
    }

    @Test(expected = ExceptionRecordNotExistsException.class)
    @Transactional
    public void testExceptionRecordNotExistsException() throws Exception {
        paymentService.updateCaseReferenceForExceptionRecord(CCD_CASE_REFERENCE_NOT_PRESENT,caseReferenceRequest);
    }

    @Test()
    @Transactional
    public void testMarkPaymentAsProcessed() throws Exception {
        EnvelopePayment envelopePayment = mockBulkScanningEnvelope().getEnvelopePayments().get(0);

        //setting mockBulkScanningEnvelope
        envelopePayment.setEnvelope(mockBulkScanningEnvelope());

        doReturn(Optional.ofNullable(envelopePayment)).when(paymentRepository).findByDcnReference(DCN_REFERENCE);
        assertThat(paymentService.updatePaymentStatus(DCN_REFERENCE, PaymentStatus.PROCESSED)).isEqualTo(DCN_REFERENCE);
    }

    @Test(expected = DcnNotExistsException.class)
    @Transactional
    public void testDcnDoesNotExistExceptionPayment() throws Exception {
        paymentService.updatePaymentStatus(CCD_CASE_REFERENCE_NOT_PRESENT, PaymentStatus.PROCESSED);
    }

    @Test()
    public void testEntityMapper() throws Exception {
        PaymentDto paymentDto = PaymentDto.paymentDtoWith().id(1)
            .paymentStatus(INCOMPLETE)
            .dcnReference("111111111111111111")
            .source("Exela")
            .dateCreated(new Date())
            .dateUpdated(new Date()).build();
        List<PaymentDto> paymentDtos = new ArrayList<>();
        paymentDtos.add(paymentDto);
        Envelope envelope = envelopeDtoMapper.toEnvelopeEntity(EnvelopeDto.envelopeDtoWith()
                                                                   .id(1)
            .dateCreated(new Date())
            .dateUpdated(new Date())
            .payments(paymentDtos)
            .paymentStatus(INCOMPLETE)
            .build());
        assertThat(envelope.getEnvelopePayments().get(0).getDcnReference()).isEqualTo("111111111111111111");
    }
}
