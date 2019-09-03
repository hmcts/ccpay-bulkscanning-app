package uk.gov.hmcts.reform.bulkscanning.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.bulkscanning.model.entity.Envelope;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopeCase;
import uk.gov.hmcts.reform.bulkscanning.model.entity.EnvelopePayment;
import uk.gov.hmcts.reform.bulkscanning.model.request.BulkScanPaymentRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static uk.gov.hmcts.reform.bulkscanning.model.enums.PaymentStatus.INCOMPLETE;

@Component
public class BulkScanPaymentRequestMapper {

    public Envelope mapEnvelopeFromBulkScanPaymentRequest(BulkScanPaymentRequest bsPaymentRequest) {

        List<EnvelopePayment> envelopePaymentList = new ArrayList<>();
        String[] dcnForPayments = bsPaymentRequest.getDocumentControlNumbers();
        //Set the DCNs from list
        Arrays.asList(dcnForPayments).stream().forEach(dcn -> envelopePaymentList.add(EnvelopePayment
                                                                                          .paymentWith()
                                                                                          .dcnReference(dcn)
                                                                                          .paymentStatus(INCOMPLETE.toString())
                                                                                          .build()));

        List<EnvelopeCase> envelopeCaseList = new ArrayList<>();

        if (bsPaymentRequest.getIsExceptionRecord()) {
            //If exception reference is present update exception reference field
            envelopeCaseList.add(EnvelopeCase.caseWith().exceptionRecordReference(bsPaymentRequest.getCcdCaseNumber()).build());
        } else {
            //If case reference present update ccd reference field
            envelopeCaseList.add(EnvelopeCase.caseWith().ccdReference(bsPaymentRequest.getCcdCaseNumber()).build());
        }

        return Envelope.envelopeWith()
            .responsibleServiceId(bsPaymentRequest.getResponsibleServiceId())
            .envelopePayments(envelopePaymentList)
            .envelopeCases(envelopeCaseList)
            .paymentStatus(INCOMPLETE.toString()) ////by default at initial status
            .build();
    }
}

