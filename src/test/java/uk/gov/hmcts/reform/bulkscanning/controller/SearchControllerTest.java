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
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;
import uk.gov.hmcts.reform.bulkscanning.service.SearchService;

import static org.mockito.ArgumentMatchers.*;
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
    public void testSearchPaymentWithCCD() throws Exception{
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("9881231111111111")
            .build();
        when(searchService.retrieveByCCDReference(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCCD_PaymentNotFound() throws Exception{
        SearchResponse searchResponse = null;
        when(searchService.retrieveByCCDReference(any(String.class)))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(404), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCCD_Exception() throws Exception{
        when(searchService.retrieveByCCDReference(any(String.class)))
            .thenThrow(new PaymentException("Exception in fetching Payments"));
        ResultActions resultActions = mockMvc.perform(get("/cases/9881231111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertTrue(resultActions.andReturn().getResponse()
                .getContentAsString().contains("Exception in fetching Payments"));
    }

    @Test
    public void testSearchPaymentWithDcn() throws Exception{
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
            .ccdReference("9881231111111111")
            .build();
        when(searchService.retrieveByDcn(anyString(), anyBoolean()))
            .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCaseDcn() throws Exception{
        SearchResponse searchResponse = SearchResponse.searchResponseWith()
                .ccdReference("9881231111111111")
                .build();
        when(searchService.retrieveByDcn(any(String.class), anyBoolean()))
                .thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/case/987123111111111111111")
                .header("ServiceAuthorization", "service")
                .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(200), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithDcn_PaymentNotFound() throws Exception{
        SearchResponse searchResponse = null;
        when(searchService.retrieveByDcn(any(String.class), anyBoolean())).thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(404), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithCaseDcn_PaymentNotFound() throws Exception{
        SearchResponse searchResponse = null;
        when(searchService.retrieveByDcn(any(String.class), anyBoolean())).thenReturn(searchResponse);
        ResultActions resultActions = mockMvc.perform(get("/case/987123111111111111111")
                .header("ServiceAuthorization", "service")
                .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(Integer.valueOf(404), Integer.valueOf(resultActions.andReturn().getResponse().getStatus()));
    }

    @Test
    public void testSearchPaymentWithDcn_Exception() throws Exception{
        when(searchService.retrieveByDcn(any(String.class), anyBoolean())).thenThrow(new PaymentException("Exception in fetching Payments"));
        ResultActions resultActions = mockMvc.perform(get("/cases")
                                                          .param("document_control_number", "987123111111111111111")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertTrue(resultActions.andReturn().getResponse()
                .getContentAsString().contains("Exception in fetching Payments"));
    }

    @Test
    public void testSearchPaymentWithCaseDcn_Exception() throws Exception{
        when(searchService.retrieveByDcn(any(String.class), anyBoolean())).thenThrow(new PaymentException("Exception in fetching Payments"));
        ResultActions resultActions = mockMvc.perform(get("/case/987123111111111111111")
                .header("ServiceAuthorization", "service")
                .accept(MediaType.APPLICATION_JSON));
        Assert.assertTrue(resultActions.andReturn().getResponse()
                .getContentAsString().contains("Exception in fetching Payments"));
    }

}
