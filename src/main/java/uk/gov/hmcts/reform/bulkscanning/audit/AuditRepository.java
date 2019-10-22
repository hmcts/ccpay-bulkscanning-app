package uk.gov.hmcts.reform.bulkscanning.audit;

import org.springframework.scheduling.annotation.Async;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.util.Map;

@Async
public interface AuditRepository {

    void trackPaymentEvent(String name, EnvelopePayment payment);

    void trackEvent(String name, Map<String, String> properties);
}
