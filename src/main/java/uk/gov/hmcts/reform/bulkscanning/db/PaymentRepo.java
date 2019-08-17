package uk.gov.hmcts.reform.bulkscanning.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.Payment;

@Repository
public interface PaymentRepo extends CrudRepository<Payment, Long> {
}
