package uk.gov.hmcts.reform.bulkscanning.model.enums;

public enum ResponsibleSiteId {
    AA07("Divorce"),
    AA08("Probate"),
    AA09("Financial Remedy");


    private String value;

    ResponsibleSiteId(String value){
        this.value = value;
    }

    public String value(){
        return value;
    }
}
