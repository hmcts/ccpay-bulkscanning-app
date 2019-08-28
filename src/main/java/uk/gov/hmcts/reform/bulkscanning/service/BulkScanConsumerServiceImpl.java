package uk.gov.hmcts.reform.bulkscanning.service;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.exception.ExceptionRecordNotExistsException;
import uk.gov.hmcts.reform.bulkscanning.mapper.BulkScanPaymentRequestMapper;
import uk.gov.hmcts.reform.bulkscanning.model.request.CaseReferenceRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeCaseRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.EnvelopeRepository;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils;

import java.util.Optional;

@Service
public class BulkScanConsumerServiceImpl implements BulkScanConsumerService{

    @Autowired
    EnvelopeRepository envelopeRepository;

    @Autowired
    BulkScanningUtils bulkScanningUtils;

    @Autowired
    BulkScanPaymentRequestMapper bsPaymentRequestMapper;

    @Autowired
    EnvelopeCaseRepository envelopeCaseRepository;

    @Override
    @Transactional
    public EnvelopeCase saveInitialMetadataFromBs(BulkScanPaymentRequest bsPaymentRequest) {
        Envelope envelopeNew = bsPaymentRequestMapper.mapEnvelopeFromBulkScanPaymentRequest(bsPaymentRequest);

        Envelope envelopeDB = bulkScanningUtils.returnExistingEnvelope(envelopeNew);

        //if we have envelope already in BS
        if (Optional.ofNullable(envelopeDB).isPresent() && Optional.ofNullable(envelopeDB.getId()).isPresent()) {
                bulkScanningUtils.handlePaymentStatus(envelopeDB, envelopeNew);
        }

        bulkScanningUtils.insertStatusHistoryAudit(envelopeDB);
        envelopeRepository.save(envelopeDB);
        return envelopeRepository.findById(envelopeDB.getId()).get();
    }

    @Override
    @Transactional
    public String updateCaseReferenceForExceptionRecord(String exceptionRecordReference, CaseReferenceRequest caseReferenceRequest) {
        //TODO yet to handle multiple envelopes with same exception reference scenario
        EnvelopeCase envelopeCase = envelopeCaseRepository.findByExceptionRecordReference(exceptionRecordReference).
            orElseThrow(ExceptionRecordNotExistsException::new);

        if (Optional.ofNullable(caseReferenceRequest).isPresent() &&
            StringUtils.isNotEmpty(caseReferenceRequest.getCcdCaseNumber())) {
            envelopeCase.setCcdReference(caseReferenceRequest.getCcdCaseNumber());
        }

        return envelopeCaseRepository.save(envelopeCase).getId().toString();
    }

    @Override
    @Transactional
    public String markPaymentAsProcessed(String dcn) {
        return envelopeRepository.save(bulkScanningUtils.markPaymentAsProcessed(dcn)).getId().toString();
    }
}
