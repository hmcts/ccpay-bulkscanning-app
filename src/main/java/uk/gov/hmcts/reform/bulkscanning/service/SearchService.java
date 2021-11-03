package uk.gov.hmcts.reform.bulkscanning.service;

import uk.gov.hmcts.reform.bulkscanning.model.response.SearchResponse;

public interface SearchService {
    SearchResponse retrieveByCcdReference(String ccdReference);

    SearchResponse retrieveBydcn(String documentControlNumber);
}
