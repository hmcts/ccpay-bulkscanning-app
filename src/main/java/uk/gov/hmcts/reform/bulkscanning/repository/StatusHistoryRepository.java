package uk.gov.hmcts.reform.bulkscanning.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
import uk.gov.hmcts.reform.bulkscanning.model.entity.StatusHistory;

import java.util.Optional;

@Repository
public interface StatusHistoryRepository extends CrudRepository<StatusHistory, Integer>, JpaSpecificationExecutor<Payment> {

    Optional<StatusHistory> findById(Integer id);

    <S extends StatusHistory> S save(S entity);
}
