package uk.gov.hmcts.reform.bulkscanning.model.enums;

public enum ResponsibleSiteId {
    AA07("Divorce"),
    AA08("Probate");

    private String value;

    ResponsibleSiteId(String value){
        this.value = value;
    }

    public String value(){
        return value;
    }
}
