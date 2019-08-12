package uk.gov.hmcts.reform.bulkscanning.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;

import java.util.Optional;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<Payment> findById(Integer id);

    Optional<Payment> findByDcnReference(String dcnReference);

    <S extends Payment> S save(S entity);
}
