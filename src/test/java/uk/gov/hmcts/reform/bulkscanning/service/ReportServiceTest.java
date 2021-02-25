package uk.gov.hmcts.reform.bulkscanning.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.bulkscanning.model.enums.EnvelopeSource.Bulk_Scan;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
public class ReportServiceTest {

    @MockBean
    PaymentRepository paymentRepository;

    @MockBean
    PaymentMetadataRepository paymentMetadataRepository;

    ReportServiceImpl reportService;

    @Before
    public void setup(){
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
        assertEquals("100.0",reportList.get(0).getAmount().toString());
    }

    @Test
    public void testRetrieveByReportTypeDataLoss() throws ParseException{
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
        assertEquals("100.0",reportList.get(0).getAmount().toString());
    }

}
