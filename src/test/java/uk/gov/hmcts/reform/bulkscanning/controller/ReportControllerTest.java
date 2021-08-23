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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.exception.PaymentException;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ResponsibleSiteId;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static uk.gov.hmcts.reform.bulkscanning.controller.PaymentControllerTest.createPaymentRequest;
import static uk.gov.hmcts.reform.bulkscanning.utils.BulkScanningUtils.asJsonString;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public class ReportControllerTest {

    MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

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
    public void testGetPaymentReportData_Unprocessed() throws Exception {
        String[] dcn = {"111122223333555511111", "111122223333555521111"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = mockMvc.perform(get("/report/data")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReportData_DataLoss() throws Exception {
        String[] dcn = {"111122223333555511111", "111122223333555521111"};
        String ccd = "1111222233335555";
        createTestReportData(ccd, dcn);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000L)));
        params.add("report_type", "DATA_LOSS");
        ResultActions resultActions = mockMvc.perform(get("/report/data")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));

        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReportData_NoData() throws Exception {

        when(reportService.retrieveDataByReportType(any(Date.class), any(Date.class), any(ReportType.class)))
            .thenReturn(null);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 30 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = mockMvc.perform(get("/report/data")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));

        Assert.assertEquals(404, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReportData_PaymentException() throws Exception {

        when(reportService.retrieveDataByReportType(any(Date.class), any(Date.class), any(ReportType.class)))
            .thenThrow(new PaymentException("PaymentException"));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 30 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = mockMvc.perform(get("/report/data")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));

        Assert.assertEquals(400, resultActions.andReturn().getResponse().getStatus());
    }


    @Test
    public void testGetPaymentReport_PaymentException() throws Exception {

        when(reportService.retrieveByReportType(any(Date.class), any(Date.class), any(ReportType.class)))
            .thenThrow(new PaymentException("PaymentException"));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 30 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        ResultActions resultActions = mockMvc.perform(get("/report/download")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));

        Assert.assertEquals(400, resultActions.andReturn().getResponse().getStatus());
    }

    @Test
    public void testGetPaymentReport() throws Exception {
        ReportData mockReportData = ReportData.recordWith()
            .amount(BigDecimal.valueOf(100))
            .build();
        List<ReportData> reportDataList = new ArrayList<>();
        reportDataList.add(mockReportData);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("date_from", getReportDate(new Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)));
        params.add("date_to", getReportDate(new Date(System.currentTimeMillis() + 30 * 60 * 60 * 1000L)));
        params.add("report_type", "UNPROCESSED");
        when(reportService.retrieveByReportType(any(Date.class), any(Date.class), any(ReportType.class)))
            .thenReturn(reportDataList);
        ResultActions resultActions = mockMvc.perform(get("/report/download")
                                                          .header("Authorization", "user")
                                                          .header("ServiceAuthorization", "service")
                                                          .params(params)
                                                          .accept(MediaType.APPLICATION_JSON));
        Assert.assertEquals(200, resultActions.andReturn().getResponse().getStatus());
    }


    private void createTestReportData(String ccd, String... dcns) throws Exception {
        //Request from Exela with one DCN

        mockMvc.perform(post("/bulk-scan-payment")
                                                          .header("ServiceAuthorization", "service")
                                                          .content(asJsonString(createPaymentRequest(dcns[0])))
                                                          .contentType(MediaType.APPLICATION_JSON));

        //Request from bulk scan with one DCN
        BulkScanPaymentRequest bulkScanPaymentRequest = createBulkScanPaymentRequest(ccd,
                                                                                     dcns, "AA08", true);

        //Post request
        mockMvc.perform(post("/bulk-scan-payment")
                                                          .header("ServiceAuthorization", "service")
                                                          .content(asJsonString(bulkScanPaymentRequest))
                                                          .contentType(MediaType.APPLICATION_JSON));
    }

    private String getReportDate(Date date) {
        DateTimeFormatter reportNameDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        return dateToLocalDateTime(date).format(reportNameDateFormat);
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static BulkScanPaymentRequest createBulkScanPaymentRequest(String ccdCaseNumber, String[] dcn, String responsibleServiceId,
                                                                      boolean isExceptionRecord) {
        return BulkScanPaymentRequest
            .createBSPaymentRequestWith()
            .ccdCaseNumber(ccdCaseNumber)
            .documentControlNumbers(dcn)
            .responsibleServiceId(ResponsibleSiteId.valueOf(responsibleServiceId).toString())
            .isExceptionRecord(isExceptionRecord)
            .build();
    }

}
