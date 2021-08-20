package uk.gov.hmcts.reform.bulkscanning.audit;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class AppInsightsAuditRepositoryTest {
    TelemetryClient telemetry;

    AppInsightsAuditRepository appInsightsAuditRepository;

    @Before
    public void setUp() {
        telemetry = spy(TelemetryClient.class);
        appInsightsAuditRepository = new AppInsightsAuditRepository("key",telemetry);
    }

    @Test
    public void testTrackEvent() {
        Map<String, String> properties = new ConcurrentHashMap<>();
        properties.put("key","value");
        appInsightsAuditRepository.trackEvent("name",properties);
        Mockito.verify(telemetry).trackEvent("name",properties,null);
    }

    @Test
    public  void testTrackPaymentEvent() {
        Map<String, String> properties = new ConcurrentHashMap<>();
        properties.put("dcnReference", "reference");
        properties.put("paymentStatus", "status");
        properties.put("source", "source");
        Envelope envelope = Envelope.envelopeWith()
                                .paymentStatus("payment-status")
                                .build();
        EnvelopePayment payment = EnvelopePayment.paymentWith()
            .dcnReference("reference")
            .paymentStatus("status")
            .source("source")
            .envelope(envelope)
            .build();
        appInsightsAuditRepository.trackPaymentEvent("name",payment);
        Mockito.verify(telemetry).trackEvent("name",properties,null);
    }

}
