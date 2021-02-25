package uk.gov.hmcts.reform.bulkscanning.exception;

import org.junit.Test;

public class DcnNotExistsExceptionTest {

    public void throwException(){
        throw new DcnNotExistsException();
    }

    @Test(expected = DcnNotExistsException.class)
    public void testThrowDcnNotExistsException(){
        throwException();
    }

}
