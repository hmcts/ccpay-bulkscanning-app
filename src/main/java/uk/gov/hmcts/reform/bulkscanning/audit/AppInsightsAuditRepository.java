package uk.gov.hmcts.reform.bulkscanning.audit;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppInsightsAuditRepository implements AuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AppInsightsAuditRepository.class);

    private final TelemetryClient telemetry;

    @Autowired
    public AppInsightsAuditRepository(@Value("${azure.application-insights.instrumentation-key}") String instrumentationKey,
                                      TelemetryClient telemetry) {
        TelemetryConfiguration.getActive().setInstrumentationKey(instrumentationKey);
        telemetry.getContext().getComponent().setVersion(getClass().getPackage().getImplementationVersion());
        this.telemetry = telemetry;
    }

    @Override
    public void trackEvent(String name, Map<String, String> properties) {
        telemetry.trackEvent(name, properties,null);
    }

    @Override
    public void trackPaymentEvent(String name, EnvelopePayment payment) {
        Map<String, String> properties = new ConcurrentHashMap<>();
        properties.put("paymentEnvelopeId", payment.getEnvelope().getId().toString());
        properties.put("dcnReference", payment.getDcnReference());
        properties.put("paymentStatus", payment.getPaymentStatus());
        properties.put("source", payment.getSource());

        LOG.info("Payment event tracked for DCN {}", payment.getDcnReference());
        telemetry.trackEvent(name, properties,null);
    }
}
