package uk.gov.hmcts.reform.bulkscanning.service;

public interface CasePaymentDeletionService {

    /**
     * Deletes all bulk scan payment data associated with the given CCD case reference.
     * Idempotent: returns successfully when no matching records exist.
     *
     * @param ccdCaseNumber 16-digit CCD case reference
     */
    void deleteByCcdCaseReference(String ccdCaseNumber);
}
