package uk.gov.hmcts.reform.bulkscanning.exception;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BulkScanCaseAlreadyExistsExceptionTest {

    public void throwException(){
        throw new BulkScanCaseAlreadyExistsException("exception message");
    }

    @Test(expected = BulkScanCaseAlreadyExistsException.class)
    public void testThrowBulkScanCaseAlreadyExistsException(){
        throwException();
    }

    @Test
    public void testThrowBulkScanCaseAlreadyExistsExceptionMessage(){
        try {
            throwException();
        }catch (BulkScanCaseAlreadyExistsException e){
            assertEquals("exception message",e.getMessage());
        }
    }
}
