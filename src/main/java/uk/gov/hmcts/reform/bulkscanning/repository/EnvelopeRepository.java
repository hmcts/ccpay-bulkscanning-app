package uk.gov.hmcts.reform.bulkscanning.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.util.Optional;

@Repository
public interface EnvelopeRepository extends CrudRepository<Envelope, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Envelope> findById(Integer id);

    <S extends Envelope> S save(S entity);
}
