package uk.gov.hmcts.reform.bulkscanning.payments;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
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

        String[] dcns = {"dcn1", "dcn2"};
        mvc.perform(post("/bulk-scan-payments")
            .content(asJsonString(CaseDCNs.builder().ccdCaseNumber("ccd1").isExceptionRecord(false).documentControlNumbers(dcns).build()))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void givenAChequeWhenItExistAlreadyThenupdateWithDetails() throws Exception {

        mvc.perform(put("/bulk-scan-payments/{dcn}", "dcn1")
            .content(asJsonString(Payment.builder()
                .amount(new BigDecimal("50.5"))
                .currency("GBP")
                .method(PaymentMethod.CHEQUE)
                .outboundBatchNumber("batch1")
                .payerName("John Crow")
                .build()
            ))
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        return objectMapper.writeValueAsString(obj);
    }
}
