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
                                                            .ccdCaseNumber("1231231231231231").isExceptionRecord(false)
                                                            .documentControlNumbers(new String[] {"123123123123123123123132"})
                                                            .responsibleServiceId("AA099").build();

        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on Invalid Site Id");
        }else{
            violations.stream().forEach(v->{
                if(v.getMessage().equals("Invalid site_id. Accepted values are AA08 or AA07 or AA09")){
                    Assertions.assertThat(v.getMessage()).isEqualTo("Invalid site_id. Accepted values are AA08 or AA07 or AA09");
                }
            });
        }
    }


    @Test
    public void testInvalidCcdCaseNumber(){
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
           .ccdCaseNumber("ccd-number").build();
        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on ccd_case_number");
        }else {
            violations.stream().forEach(v -> {
                if (v.getMessage().equals("ccd_case_number should be numeric")) {
                    Assertions.assertThat(v.getMessage()).isEqualTo("ccd_case_number should be numeric");
                }
            });
        }
    }

    @Test
    public void testCcdCaseNumberWithGreaterThe16Digits(){
        BulkScanPaymentRequest bulkScanPaymentRequest = BulkScanPaymentRequest.createBSPaymentRequestWith()
            .ccdCaseNumber("23213213213123213213").build();
        Set<ConstraintViolation<BulkScanPaymentRequest>> violations = validator.validate(bulkScanPaymentRequest);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on ccd_case_number");
        }else {
            violations.stream().forEach(v -> {
                if (v.getMessage().equals("ccd_case_number length must be 16 digits")) {
                    Assertions.assertThat(v.getMessage()).isEqualTo("ccd_case_number length must be 16 digits");
                }
            });
        }
    }


}
