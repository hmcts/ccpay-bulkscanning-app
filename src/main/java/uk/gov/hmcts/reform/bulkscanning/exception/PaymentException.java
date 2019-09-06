package uk.gov.hmcts.reform.bulkscanning.exception;

public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
    public PaymentException(Throwable cause) {
        super(cause);
    }
}
