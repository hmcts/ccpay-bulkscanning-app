package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.PaymentMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMetadataRepository extends CrudRepository<PaymentMetadata, Integer>, JpaSpecificationExecutor<PaymentMetadata> {
    Optional<PaymentMetadata> findByDcnReference(String dcnReference);

    @Query("select pm from PaymentMetadata pm where pm.dcnReference in :dcnReferences")
    List<PaymentMetadata> findAllByDcnReferenceIn(Collection<String> dcnReferences);

    long deleteByDcnReference(String dcnReference);
}
