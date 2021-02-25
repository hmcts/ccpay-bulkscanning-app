package uk.gov.hmcts.reform.bulkscanning.model.request;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SearchRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeClass
    public static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterClass
    public static void close() {
        validatorFactory.close();
    }

    @Test
    public void testNonNumericCcdReference(){
        SearchRequest searchRequest = SearchRequest.searchRequestWith()
                                            .ccdReference("ccd-reference")
                                            .build();
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(searchRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_reference should be numeric")){
                assertEquals("ccd_reference should be numeric",v.getMessage());
            }
        });
    }

    @Test
    public void testCcdReferenceWithLargerThanMaxDigits(){
        SearchRequest searchRequest = SearchRequest.searchRequestWith()
            .ccdReference("2131232132131232312123")
            .build();
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(searchRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_reference length must be 16 Characters")){
                assertEquals("ccd_reference length must be 16 Characters",v.getMessage());
            }
        });
    }

    @Test
    public void testEmptyExceptionRecordAndDCN(){
        SearchRequest searchRequest = SearchRequest.searchRequestWith()
            .exceptionRecord("")
            .documentControlNumber("")
            .build();
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(searchRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("exception_record can't be Blank")){
                assertEquals("exception_record can't be Blank",v.getMessage());
            }
            if(v.getMessage().equals("document_control_number can't be Blank")){
                assertEquals("document_control_number can't be Blank",v.getMessage());
            }
        });
    }
}
