package uk.gov.hmcts.reform.bulkscanning.model.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;

import java.util.Optional;

@Repository
public interface EnvelopeCaseRepository extends CrudRepository<EnvelopeCase, Integer>, JpaSpecificationExecutor<Envelope> {

    Optional<EnvelopeCase> findByCcdReference(String ccdReference);

    Optional<EnvelopeCase> findByExceptionRecordReference(String exceptionRecordReference);

    Optional<EnvelopeCase> findByEnvelopeId(Integer envelopeId);
}
