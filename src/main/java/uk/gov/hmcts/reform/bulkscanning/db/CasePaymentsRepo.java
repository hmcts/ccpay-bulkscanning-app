package uk.gov.hmcts.reform.bulkscanning.db;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.CasePayment;

@Repository
public interface CasePaymentsRepo extends CrudRepository<CasePayment, String> {
}
