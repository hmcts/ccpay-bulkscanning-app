package uk.gov.hmcts.reform.bulkscanning.exception;

public class BulkScanCaseAlreadyExistsException extends RuntimeException{
    public BulkScanCaseAlreadyExistsException(String message) {
        super(message);
    }
}
