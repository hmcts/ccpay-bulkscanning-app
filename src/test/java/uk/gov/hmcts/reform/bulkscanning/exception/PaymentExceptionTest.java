package uk.gov.hmcts.reform.bulkscanning.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PaymentExceptionTest {
    public void throwException(){
        throw new PaymentException("payment exception");
    }

    @Test(expected = PaymentException.class)
    public void testPaymentException(){
        throwException();
    }

    @Test
    public void testPaymentExceptionMessage(){
        try{
            throwException();
        }catch (PaymentException e){
            assertEquals("payment exception",e.getMessage());
        }
    }

    @Test
    public void testPaymentExceptionCause(){
        Throwable expectedCause = new Throwable();
        try{
            throw new PaymentException(expectedCause);
        }catch (PaymentException e){
            assertEquals(expectedCause,e.getCause());
        }
    }
}
