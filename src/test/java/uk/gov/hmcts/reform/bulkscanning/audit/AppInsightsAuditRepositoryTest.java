package uk.gov.hmcts.reform.bulkscanning.audit;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMapOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class AppInsightsAuditRepositoryTest {
    TelemetryClient telemetry;

    AppInsightsAuditRepository appInsightsAuditRepository;

    @Before
    public void setup(){
        telemetry = spy(TelemetryClient.class);
        appInsightsAuditRepository = new AppInsightsAuditRepository("key",telemetry);
    }

    @Test
    public void testTrackEvent(){
        Map<String, String> properties = new HashMap<>();
        properties.put("key","value");
        appInsightsAuditRepository.trackEvent("name",properties);
        Mockito.verify(telemetry).trackEvent("name",properties,null);
    }

    @Test
    public  void testTrackPaymentEvent(){
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
            .put("dcnReference", "reference")
            .put("paymentStatus", "status")
            .put("source", "source")
            .build();
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
