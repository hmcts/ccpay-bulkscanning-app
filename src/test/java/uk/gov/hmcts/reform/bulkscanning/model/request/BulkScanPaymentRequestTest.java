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

public class BulkScanPaymentRequestTest {
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
    public void testResponsibleServiceId(){
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
                                                            .responsibleServiceId("AA099").build();
        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("site_id length must be 4 Characters")){
                assertEquals("site_id length must be 4 Characters",v.getMessage());
            }
            if(v.getMessage().equals("Invalid site_id. Accepted values are AA08 or AA07 or AA09")){
                assertEquals("Invalid site_id. Accepted values are AA08 or AA07 or AA09",v.getMessage());
            }
        });
    }


    @Test
    public void testInvalidCcdCaseNumber(){
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
           .ccdCaseNumber("ccd-number").build();
        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_case_number should be numeric")){
                assertEquals("ccd_case_number should be numeric",v.getMessage());
            }
        });
    }

    @Test
    public void testCcdCaseNumberWithGreaterThe16Digits(){
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
            .ccdCaseNumber("23213213213123213213").build();
        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("ccd_case_number length must be 16 digits")){
                assertEquals("ccd_case_number length must be 16 digits",v.getMessage());
            }
        });
    }


}
