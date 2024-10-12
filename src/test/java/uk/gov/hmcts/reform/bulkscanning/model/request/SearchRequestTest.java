package uk.gov.hmcts.reform.bulkscanning.model.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.fail;

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
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on ccd_case_number");
        }else {
            violations.stream().forEach(v->{
                if("ccd_reference should be numeric".equals(v.getMessage())){
                    Assertions.assertThat(v.getMessage()).isEqualTo("ccd_reference should be numeric");
                }
            });
        }
    }

    @Test
    public void testCcdReferenceWithLargerThanMaxDigits(){
        SearchRequest searchRequest = SearchRequest.searchRequestWith()
            .ccdReference("2131232132131232312123")
            .build();
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(searchRequest);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on ccd_reference");
        }else {
            violations.stream().forEach(v->{
                if("ccd_reference length must be 16 Characters".equals(v.getMessage())){
                    Assertions.assertThat(v.getMessage()).isEqualTo("ccd_reference length must be 16 Characters");
                }
            });
        }
    }

    @Test
    public void testEmptyExceptionRecordAndDCN(){
        SearchRequest searchRequest = SearchRequest.searchRequestWith()
            .exceptionRecord("")
            .documentControlNumber("")
            .build();
        Set<ConstraintViolation<SearchRequest>> violations = validator.validate(searchRequest);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on exception_record");
        }else {
            violations.stream().forEach(v->{
                if("exception_record can't be Blank".equals(v.getMessage())){
                    Assertions.assertThat(v.getMessage()).isEqualTo("exception_record can't be Blank");
                }
                if("document_control_number can't be Blank".equals(v.getMessage())){
                    Assertions.assertThat(v.getMessage()).isEqualTo("document_control_number can't be Blank");
                }
            });
        }
    }
}
