package uk.gov.hmcts.reform.bulkscanning.model.dto;

import lombok.Data;

@Data
public class BaseReportData {
    private String respServiceId;
    private String paymentMethod;
    private String dateBanked;
}
