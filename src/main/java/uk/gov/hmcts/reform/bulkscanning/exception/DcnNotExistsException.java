package uk.gov.hmcts.reform.bulkscanning.exception;

public class DcnNotExistsException extends RuntimeException{
    public DcnNotExistsException(){super();};
    public DcnNotExistsException(String message) {
        super(message);
    }
}
