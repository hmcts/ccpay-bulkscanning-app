package uk.gov.hmcts.reform.bulkscanning.model.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BulkScanPaymentTest {
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
    public void testSizeOfDCNRefernce(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .dcnReference("34323423324234342343243423").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on document_control_number");
        }else {
            violations.stream().forEach(v->{
                if("document_control_number length must be 21 digits.".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("document_control_number length must be 21 digits.");
                }
            });
        }
    }

    @Test
    public void testDecimalsOfAmount(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .amount(BigDecimal.valueOf(100.0232)).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on amount");
        }else {
            violations.stream().forEach(v->{
                if("amount cannot have more than 2 decimal places".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("amount cannot have more than 2 decimal places");
                }
            });
        }

    }

    @Test
    public void testEmptyAmount(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .amount(BigDecimal.valueOf(0)).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on amount");
        }else {
            violations.stream().forEach(v->{
                if("amount must be greater than or equal to 0.01".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("amount must be greater than or equal to 0.01");
                }
            });
        }
    }

    @Test
    public void testEmptyCurrency(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .currency("").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on Currency");
        }else {
            violations.stream().forEach(v->{
                if("currency can't be Blank".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("currency can't be Blank");
                }
            });
        }

    }

    @Test
    public void testBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .bankGiroCreditSlipNumber(213).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on bank_giro_credit_slip_number");
        }else {
            violations.stream().forEach(v->{
                if("bank_giro_credit_slip_number can't be Blank".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("bank_giro_credit_slip_number can't be Blank");
                }
            });
        }
    }

    @Test
    public void testPositiveBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .bankGiroCreditSlipNumber(-1).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on bank_giro_credit_slip_number");
        }else {
            violations.stream().forEach(v->{
                if("bank_giro_credit_slip_number must be Positive".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("bank_giro_credit_slip_number must be Positive");
                }
            });
        }
    }

    @Test
    public void testLengthOfBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                                .bankGiroCreditSlipNumber(-1).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on bank_giro_credit_slip_number");
        }else {
            violations.stream().forEach(v->{
                if("bank_giro_credit_slip_number length must not be greater than 6 digits".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("bank_giro_credit_slip_number length must not be greater than 6 digits");
                }
            });
        }
    }

    @Test
    public void testBankedDate(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                                .bankedDate("").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on banked_date");
        }else {
            violations.stream().forEach(v->{
                if("banked_date can't be Blank".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("banked_date can't be Blank");
                }
            });
        }

    }

    @Test
    public void testFutureBankDate(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .bankedDate("2021-11-01").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on banked_date");
        }else {
            violations.stream().forEach(v->{
                if("Invalid banked_Date. Date format should be YYYY-MM-DD (e.g. 2019-01-01). should never be a future date".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("Invalid banked_Date. Date format should be YYYY-MM-DD (e.g. 2019-01-01). should never be a future date");
                }
            });
        }
    }

    @Test
    public void testInvalidCashMethod(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .method("invalid-method").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on Cash/Cheque/PostalOrder");
        }else {
            violations.stream().forEach(v->{
                if("Invalid method. Accepted value Cash/Cheque/PostalOrder".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("Invalid method. Accepted value Cash/Cheque/PostalOrder");
                }
            });
        }
    }

    @Test
    public void testValidCurrency(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .currency("GBP").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        if(violations.isEmpty()){
            fail("should have thrown an Error Message on Invalid currency");
        }else {
            violations.stream().forEach(v->{
                if("Invalid currency. Accepted value GBP".equals(v.getMessage())){
                    assertThat(v.getMessage()).isEqualTo("Invalid currency. Accepted value GBP");
                }
            });
        }
    }
}
