package uk.gov.hmcts.reform.bulkscanning.exception;

public class PaymentDeletionException extends RuntimeException {

    public PaymentDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaymentDeletionException(String message) {
        super(message);
    }
}
