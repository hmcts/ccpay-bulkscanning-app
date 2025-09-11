package uk.gov.hmcts.reform.bulkscanning.audit;

import com.microsoft.applicationinsights.TelemetryClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AppInsightsAuditRepository implements AuditRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AppInsightsAuditRepository.class);

    private final TelemetryClient telemetry;

    @Override
    public void trackEvent(String name, Map<String, String> properties) {
        telemetry.trackEvent(name, properties,null);
    }

    @Override
    public void trackPaymentEvent(String name, EnvelopePayment payment) {
        if(Optional.ofNullable(payment.getSource()).isPresent()
                && Optional.ofNullable(payment.getPaymentStatus()).isPresent()
                && Optional.ofNullable(payment.getDcnReference()).isPresent()){
            Map<String, String> properties = new ConcurrentHashMap<>();
            properties.put("dcnReference", payment.getDcnReference());
            properties.put("paymentStatus", payment.getPaymentStatus());
            properties.put("source", payment.getSource());

            LOG.info("Payment event tracked for DCN {}", payment.getDcnReference());
            telemetry.trackEvent(name, properties,null);
        }
    }
}
