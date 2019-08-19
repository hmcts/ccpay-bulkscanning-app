package uk.gov.hmcts.reform.bulkscanning.payments;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCNs;
import uk.gov.hmcts.reform.bulkscanning.model.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.PaymentMethod;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    locations = "classpath:application-test.properties")
@SuppressWarnings("PMD")
public class PaymentTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void givenAChequeWhenItDoesntExistAlreadyThenCreateNew() throws Exception {

        String dcns[] = {"dcn1", "dcn2"};
        mvc.perform(post("/bulk-scan-payments")
            .content(asJsonString(CaseDCNs.builder().ccdCaseNumber("ccd1").isExceptionRecord(false).documentControlNumbers(dcns)))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void givenAChequeWhenItExistAlreadyThenupdateWithDetails() throws Exception {

        String dcns[] = {"dcn1", "dcn2"};
        mvc.perform(post("/bulk-scan-payments")
            .content(asJsonString(Payment.builder()
                .dcnPayment("dcn1")
                .amount(new BigDecimal("50.5"))
                .bankedDate(LocalDate.now())
                .currency("GBP")
                .method(PaymentMethod.CHEQUE)
                .outboundBatchNumber("batch1")
                .payerName("John Crow")
            ))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    public static String asJsonString(final Object obj) {
        try {
            
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
