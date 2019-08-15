package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.dto.BulkScanPaymentRequest;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class BulkScanPaymentRequestMapper {

    public Envelope mapEnvelopeFromBSPaymentRequest(BulkScanPaymentRequest bsPaymentRequest) {

        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();
        String[] dcnForPayments = bsPaymentRequest.getDocumentControlNumbers();
        //Set the DCNs from list
        Arrays.asList(dcnForPayments).stream().forEach(dcn -> envelopePaymentList.add(EnvelopePayment
            .paymentWith()
            .dcnReference(dcn)
            .paymentStatus(PaymentStatus.INCOMPLETE.toString()) //by default at initial status
            .build()));

        List<EnvelopeCase> envelopeCaseList = new ArrayList<>();
        EnvelopeCase envelopeCase = null;

        if (StringUtils.equalsIgnoreCase(bsPaymentRequest.getIsExceptionRecord(),"false")) {
            //If case reference present update ccd reference field
            envelopeCase = EnvelopeCase.caseWith().ccdReference(bsPaymentRequest.getCcdCaseNumber()).build();
        } else if (StringUtils.equalsIgnoreCase(bsPaymentRequest.getIsExceptionRecord(),"true")) {
            //If exception reference is present update exception reference field
            envelopeCase = EnvelopeCase.caseWith().exceptionRecordReference(bsPaymentRequest.getCcdCaseNumber()).build();
        }
        envelopeCaseList.add(envelopeCase);

        return Envelope.envelopeWith()
            .responsibleServiceId(bsPaymentRequest.getResponsibleServiceId())
            .envelopePayments(envelopePaymentList)
            .envelopeCases(envelopeCaseList)
            .paymentStatus(PaymentStatus.INCOMPLETE.toString()) ////by default at initial status
            .build();
    }
}

