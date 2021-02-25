package uk.gov.hmcts.reform.bulkscanning.exception;

import org.junit.Test;

public class ExceptionRecordNotExistsExceptionTest {
    public void throwException(){
        throw new ExceptionRecordNotExistsException();
    }

    @Test(expected = ExceptionRecordNotExistsException.class)
    public void testThrowDExceptionRecordNotExistsException(){
        throwException();
    }

}
