package uk.gov.hmcts.reform.bulkscanning.controller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.SearchService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public class SearchControllerTest {

    MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Transactional
    public void testSearchPaymentWithCCD() throws Exception {
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("9881231111111111")
            .build();
        when(searchService.retrieveByCcdReference(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCCD_PaymentNotFound() throws Exception {
        SearchResponse searchResponse = null;
        when(searchService.retrieveByCcdReference(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(404), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCCD_Exception() throws Exception {
        when(searchService.retrieveByCcdReference(any(String.class)))
            .thenThrow(new PaymentException("Exception in fetching Payments"));
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(true, resultActions.andReturn().getResponse()
            .getContentAsString().contains("Exception in fetching Payments"));
    }

    @Test
    @Transactional
    public void testSearchPaymentWithdcn() throws Exception {
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("9881231111111111")
            .build();
        when(searchService.retrieveBydcn(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithdcn_PaymentNotFound() throws Exception {
        SearchResponse searchResponse = null;
        when(searchService.retrieveBydcn(any(String.class))).thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(404), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithdcn_Exception() throws Exception{
        when(searchService.retrieveBydcn(any(String.class))).thenThrow(new PaymentException("Exception in fetching Payments"));
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(true, resultActions.andReturn().getResponse()
            .getContentAsString().contains("Exception in fetching Payments"));
    }

}
