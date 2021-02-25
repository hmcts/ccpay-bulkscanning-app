package uk.gov.hmcts.reform.bulkscanning.model.request;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        violations.stream().forEach(v->{
            if(v.getMessage().equals("document_control_number length must be 21 digits.")){
                assertEquals("document_control_number length must be 21 digits.",v.getMessage());
            }
        });
    }

    @Test
    public void testDecimalsOfAmount(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .amount(BigDecimal.valueOf(100.0232)).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("amount cannot have more than 2 decimal places")){
                assertEquals("amount cannot have more than 2 decimal places",v.getMessage());
            }
        });
    }

    @Test
    public void testEmptyAmount(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .amount(BigDecimal.valueOf(0)).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("amount must be greater than or equal to 0.01")){
                assertEquals("amount must be greater than or equal to 0.01",v.getMessage());
            }
        });
    }

    @Test
    public void testEmptyCurrency(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .currency("").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("currency can't be Blank")){
                assertEquals("currency can't be Blank",v.getMessage());
            }
        });
    }

    @Test
    public void testBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .bankGiroCreditSlipNumber(213).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("bank_giro_credit_slip_number can't be Blank")){
                assertEquals("bank_giro_credit_slip_number can't be Blank",v.getMessage());
            }
        });
    }

    @Test
    public void testPositiveBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .bankGiroCreditSlipNumber(-1).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("bank_giro_credit_slip_number must be Positive")){
                assertEquals("bank_giro_credit_slip_number must be Positive",v.getMessage());
            }
        });
    }

    @Test
    public void testLengthOfBankGiroCreditSlipNumber(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                                .bankGiroCreditSlipNumber(-1).build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("bank_giro_credit_slip_number length must not be greater than 6 digits")){
                assertEquals("bank_giro_credit_slip_number length must not be greater than 6 digits",v.getMessage());
            }
        });
    }

    @Test
    public void testBankedDate(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                                .bankedDate("").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("banked_date can't be Blank")){
                assertEquals("banked_date can't be Blank",v.getMessage());
            }
        });
    }

    @Test
    public void testFutureBankDate(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
                                            .bankedDate("2021-11-01").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("Invalid banked_Date. Date format should be YYYY-MM-DD (e.g. 2019-01-01). should never be a future date")){
                assertEquals("Invalid banked_Date. Date format should be YYYY-MM-DD (e.g. 2019-01-01). should never be a future date",v.getMessage());
            }
        });
    }

    @Test
    public void testInvalidCashMethod(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .method("invalid-method").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("Invalid method. Accepted value Cash/Cheque/PostalOrder")){
                assertEquals("Invalid method. Accepted value Cash/Cheque/PostalOrder",v.getMessage());
            }
        });
    }

    @Test
    public void testValidCurrency(){
        BulkScanPayment bulkScanPayment = BulkScanPayment.createPaymentRequestWith()
            .currency("GBP").build();
        Set<ConstraintViolation<BulkScanPayment>> violations = validator.validate(bulkScanPayment);
        violations.stream().forEach(v->{
            if(v.getMessage().equals("Invalid currency. Accepted value GBP")){
                assertEquals("Invalid currency. Accepted value GBP",v.getMessage());
            }
        });
    }
}
