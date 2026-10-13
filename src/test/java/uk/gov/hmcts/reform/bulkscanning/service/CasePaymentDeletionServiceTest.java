package uk.gov.hmcts.reform.bulkscanning.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentDeletionException;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.StatusHistoryRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CasePaymentDeletionServiceTest {

    private static final String CCD_CASE_NUMBER = "1747738470383168";
    private static final String DCN_REFERENCE = "111122223333444411111";
    private static final Integer ENVELOPE_ID = 123;

    @Mock
    private EnvelopeCaseRepository envelopeCaseRepository;

    @Mock
    private EnvelopeRepository envelopeRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentMetadataRepository paymentMetadataRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    private CasePaymentDeletionService casePaymentDeletionService;

    @Before
    public void setUp() {
        casePaymentDeletionService = new CasePaymentDeletionServiceImpl(
            envelopeCaseRepository,
            envelopeRepository,
            paymentRepository,
            paymentMetadataRepository,
            statusHistoryRepository
        );
    }

    @Test
    public void testDeleteByCcdCaseReference_noRecords_isIdempotent() {
        when(envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER)).thenReturn(Optional.empty());

        casePaymentDeletionService.deleteByCcdCaseReference(CCD_CASE_NUMBER);

        verify(statusHistoryRepository, never()).deleteByEnvelopeId(anyInt());
        verify(paymentRepository, never()).deleteByEnvelopeId(anyInt());
        verify(envelopeCaseRepository, never()).deleteByCcdReference(anyString());
        verify(envelopeRepository, never()).deleteById(anyInt());
    }

    @Test
    public void testDeleteByCcdCaseReference_deletesRelatedRecordsInOrder() {
        Envelope envelope = Envelope.envelopeWith().id(ENVELOPE_ID).build();
        EnvelopeCase envelopeCase = EnvelopeCase.caseWith()
            .id(1)
            .ccdReference(CCD_CASE_NUMBER)
            .envelope(envelope)
            .build();
        EnvelopePayment payment = EnvelopePayment.paymentWith()
            .id(1)
            .dcnReference(DCN_REFERENCE)
            .envelope(envelope)
            .build();

        when(envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER))
            .thenReturn(Optional.of(List.of(envelopeCase)));
        when(statusHistoryRepository.deleteByEnvelopeId(ENVELOPE_ID)).thenReturn(1L);
        when(paymentRepository.findByEnvelopeId(ENVELOPE_ID)).thenReturn(Optional.of(List.of(payment)));
        when(paymentMetadataRepository.deleteByDcnReferenceIn(List.of(DCN_REFERENCE))).thenReturn(1L);
        when(paymentRepository.deleteByEnvelopeId(ENVELOPE_ID)).thenReturn(1L);
        when(envelopeCaseRepository.deleteByCcdReference(CCD_CASE_NUMBER)).thenReturn(1L);

        casePaymentDeletionService.deleteByCcdCaseReference(CCD_CASE_NUMBER);

        verify(statusHistoryRepository, times(1)).deleteByEnvelopeId(ENVELOPE_ID);
        verify(paymentMetadataRepository, times(1)).deleteByDcnReferenceIn(List.of(DCN_REFERENCE));
        verify(paymentRepository, times(1)).deleteByEnvelopeId(ENVELOPE_ID);
        verify(envelopeCaseRepository, times(1)).deleteByCcdReference(CCD_CASE_NUMBER);
        verify(envelopeRepository, times(1)).deleteById(ENVELOPE_ID);
    }

    @Test
    public void testDeleteByCcdCaseReference_emptyList_isIdempotent() {
        when(envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER))
            .thenReturn(Optional.of(Collections.emptyList()));

        casePaymentDeletionService.deleteByCcdCaseReference(CCD_CASE_NUMBER);

        verify(envelopeCaseRepository, never()).deleteByCcdReference(anyString());
        verify(envelopeRepository, never()).deleteById(anyInt());
    }

    @Test
    public void testDeleteByCcdCaseReference_unexpectedError_throwsPaymentDeletionException() {
        when(envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER))
            .thenThrow(new RuntimeException("DB failure"));

        PaymentDeletionException exception = assertThrows(
            PaymentDeletionException.class,
            () -> casePaymentDeletionService.deleteByCcdCaseReference(CCD_CASE_NUMBER)
        );

        assertEquals(
            "Unexpected error occurred during deletion for CCD case reference: " + CCD_CASE_NUMBER,
            exception.getMessage()
        );
    }

    @Test
    public void testDeleteByCcdCaseReference_deleteEnvelopeFails_throwsPaymentDeletionException() {
        Envelope envelope = Envelope.envelopeWith().id(ENVELOPE_ID).build();
        EnvelopeCase envelopeCase = EnvelopeCase.caseWith()
            .id(1)
            .ccdReference(CCD_CASE_NUMBER)
            .envelope(envelope)
            .build();

        when(envelopeCaseRepository.findByCcdReference(CCD_CASE_NUMBER))
            .thenReturn(Optional.of(List.of(envelopeCase)));
        when(statusHistoryRepository.deleteByEnvelopeId(ENVELOPE_ID)).thenReturn(0L);
        when(paymentRepository.findByEnvelopeId(ENVELOPE_ID)).thenReturn(Optional.empty());
        when(paymentRepository.deleteByEnvelopeId(ENVELOPE_ID)).thenReturn(0L);
        when(envelopeCaseRepository.deleteByCcdReference(CCD_CASE_NUMBER)).thenReturn(1L);
        doThrow(new RuntimeException("Unable to delete envelope")).when(envelopeRepository).deleteById(ENVELOPE_ID);

        assertThrows(
            PaymentDeletionException.class,
            () -> casePaymentDeletionService.deleteByCcdCaseReference(CCD_CASE_NUMBER)
        );
    }
}
