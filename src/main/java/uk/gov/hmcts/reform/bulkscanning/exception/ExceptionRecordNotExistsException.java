package uk.gov.hmcts.reform.bulkscanning.exception;

public class ExceptionRecordNotExistsException extends RuntimeException{
    public ExceptionRecordNotExistsException(){super();};
    public ExceptionRecordNotExistsException(String message) {
        super(message);
    }
}
