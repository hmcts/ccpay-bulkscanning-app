package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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

    @Query("""
        select distinct ep
        from EnvelopePayment ep
        left join fetch ep.envelope e
        left join fetch e.envelopeCases
        where ep.paymentStatus = :paymentStatus
          and ep.dateCreated between :fromDate and :toDate
        """)
    Optional<List<EnvelopePayment>> findForReportByPaymentStatusAndDateCreatedBetween(
        String paymentStatus,
        LocalDateTime fromDate,
        LocalDateTime toDate
    );

    long deleteByDcnReference(String dcnReference);
}
