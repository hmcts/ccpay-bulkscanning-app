package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Payment;
@Repository
public interface PaymentRepository extends CrudRepository<Payment, Integer> {
}
