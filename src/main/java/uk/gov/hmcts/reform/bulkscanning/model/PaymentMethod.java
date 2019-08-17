package uk.gov.hmcts.reform.bulkscanning.model;

public enum PaymentMethod {

    CASH("cash"),
    CHEQUE("cheque"),
    POSTAL_ORDER("postal order");

    String type;

    PaymentMethod(String type) {
        this.type = type;
    }

    public String getType() {
         return type;
    }
}
