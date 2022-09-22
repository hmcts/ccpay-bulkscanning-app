package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;

public interface SearchService {
    SearchResponse retrieveByCCDReference(String ccdReference);
    SearchResponse retrieveByDcn(String documentControlNumber, boolean internalFlag);
}
