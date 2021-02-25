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

public class CaseReferenceRequestTest {
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
    public void testBlankCcdCaseNumber(){
        CaseReferenceRequest caseReferenceRequest  = CaseReferenceRequest.createCaseReferenceRequest()
                                                        .ccdCaseNumber("").build();
        Set<ConstraintViolation<CaseReferenceRequest>> violations = validator.validate(caseReferenceRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_case_number can't be Blank")){
                assertEquals("ccd_case_number can't be Blank",v.getMessage());
            }
        });
    }

    @Test
    public void testNonNumericCcdCaseNumber(){
        CaseReferenceRequest caseReferenceRequest  = CaseReferenceRequest.createCaseReferenceRequest()
            .ccdCaseNumber("ewrwerewre").build();
        Set<ConstraintViolation<CaseReferenceRequest>> violations = validator.validate(caseReferenceRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_case_number should be numeric")){
                assertEquals("ccd_case_number should be numeric",v.getMessage());
            }
        });
    }

    @Test
    public void testCcdCaseNumberWithLargerDigits(){
        CaseReferenceRequest caseReferenceRequest  = CaseReferenceRequest.createCaseReferenceRequest()
            .ccdCaseNumber("2312312321312342122").build();
        Set<ConstraintViolation<CaseReferenceRequest>> violations = validator.validate(caseReferenceRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_case_number length must be 16 Characters")){
                assertEquals("ccd_case_number length must be 16 Characters",v.getMessage());
            }
        });
    }
}
