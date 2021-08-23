package uk.gov.hmcts.reform.bulkscanning.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.bulkscanning.model.dto.ReportData;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;
import uk.gov.hmcts.reform.bulkscanning.model.enums.ReportType;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentMetadataRepository;
import uk.gov.hmcts.reform.bulkscanning.model.repository.PaymentRepository;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Bulk_Scan;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"local", "test"})
public class ReportServiceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    PaymentRepository paymentRepository;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private JwtDecoder jwtDecoder;


    @MockBean
    PaymentMetadataRepository paymentMetadataRepository;

    ReportServiceImpl reportService;

    @Before
    public void setUp() {
        reportService = new ReportServiceImpl(paymentRepository,paymentMetadataRepository);
    }

    @Test
    public void testRetrieveByReportTypeUnprocessed() throws ParseException {
        List<EnvelopePayment> envelopePayments = new ArrayList<>();
        EnvelopePayment envelopePayment = EnvelopePayment.paymentWith()
                                            .dcnReference("dcnReference")
                                            .dateCreated(LocalDateTime.of(2021,1,15,10,10))
                                            .build();
        envelopePayments.add(envelopePayment);
        when(paymentRepository.findByPaymentStatus(any(String.class))).thenReturn(Optional.of(envelopePayments));


        PaymentMetadata paymentMetadata = PaymentMetadata.paymentMetadataWith()
                                                .dateBanked(LocalDateTime.now())
                                                .bgcReference("bgc-reference")
                                                .paymentMethod("payment-method")
                                                .amount(BigDecimal.valueOf(100.00))
                                                .build();
        when(paymentMetadataRepository.findByDcnReference(any(String.class))).thenReturn(Optional.ofNullable(paymentMetadata));
        String startString = "January 10, 2021";
        String endString = "January 18, 2021";
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date startDate = format.parse(startString);
        Date endDate = format.parse(endString);
        List<ReportData> reportList = reportService.retrieveByReportType(startDate, endDate, ReportType.UNPROCESSED);
        assertThat(reportList.get(0).getAmount().toString()).isEqualTo("100.0");
    }

    @Test
    @Transactional
    public void testRetrieveByReportTypeDataLoss() throws ParseException {
        List<EnvelopePayment> envelopePayments = new ArrayList<>();
        EnvelopePayment envelopePayment = EnvelopePayment.paymentWith()
            .dcnReference("dcnReference").source(Bulk_Scan.toString())
            .dateCreated(LocalDateTime.of(2021,1,15,10,10))
            .build();
        envelopePayments.add(envelopePayment);
        when(paymentRepository.findByPaymentStatus(any(String.class))).thenReturn(Optional.of(envelopePayments));
        PaymentMetadata paymentMetadata = PaymentMetadata.paymentMetadataWith()
            .dateBanked(LocalDateTime.now())
            .bgcReference("bgc-reference")
            .paymentMethod("payment-method")
            .amount(BigDecimal.valueOf(100.00))
            .build();
        when(paymentMetadataRepository.findByDcnReference(any(String.class))).thenReturn(Optional.ofNullable(paymentMetadata));
        String startString = "January 10, 2021";
        String endString = "January 18, 2021";
        DateFormat format = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        Date startDate = format.parse(startString);
        Date endDate = format.parse(endString);
        List<ReportData> reportList = reportService.retrieveByReportType(startDate, endDate, ReportType.DATA_LOSS);
        assertThat(reportList.get(0).getAmount().toString()).isEqualTo("100.0");
    }

}
