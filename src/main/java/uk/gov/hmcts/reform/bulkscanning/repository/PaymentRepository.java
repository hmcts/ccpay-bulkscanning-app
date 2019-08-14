package uk.gov.hmcts.reform.bulkscanning.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findById(Integer id);

    Optional<Payment> findByDcnReference(String dcnReference);

    Optional<List<Payment>> findByEnvelopeId(Integer envelopeId);

    <S extends Payment> S save(S entity);
}
