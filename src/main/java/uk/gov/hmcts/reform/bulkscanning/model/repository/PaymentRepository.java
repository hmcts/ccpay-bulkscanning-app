package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends CrudRepository<EnvelopePayment, Integer>, JpaSpecificationExecutor<EnvelopePayment> {

    Optional<EnvelopePayment> findByDcnReference(String dcnReference);

    Optional<List<EnvelopePayment>> findByEnvelopeId(Integer envelopeId);

    Optional<List<EnvelopePayment>> findByPaymentStatusAndDateCreatedBetween(
        String paymentStatus,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );

    long deleteByDcnReference(String dcnReference);
}
