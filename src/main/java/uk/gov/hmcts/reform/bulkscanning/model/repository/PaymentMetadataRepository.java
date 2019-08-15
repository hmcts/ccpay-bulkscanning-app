package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;

@Repository
public interface PaymentMetadataRepository extends CrudRepository<PaymentMetadata, Integer>, JpaSpecificationExecutor<PaymentMetadata> {
}
