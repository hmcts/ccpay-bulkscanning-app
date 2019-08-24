package uk.gov.hmcts.reform.bulkscanning.db;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCN;

@Repository
public interface CasePaymentsRepo extends CrudRepository<CaseDCN, String> {
    List<CaseDCN> findByCcdCaseNumber(String ccdCaseNumber);
}
