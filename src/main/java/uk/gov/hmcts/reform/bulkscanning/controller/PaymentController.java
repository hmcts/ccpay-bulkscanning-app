package uk.gov.hmcts.reform.bulkscanning.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanning.db.CasePaymentsRepo;
import uk.gov.hmcts.reform.bulkscanning.db.PaymentRepo;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCN;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCNs;
import uk.gov.hmcts.reform.bulkscanning.model.Payment;

@RestController
public class PaymentController {

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private CasePaymentsRepo casePaymentsRepo;
    @PutMapping("/bulk-scan-payments/{dcn}")
    public void savePayment(@PathVariable String dcn, @RequestBody Payment payment){

        payment.setDcnPayment(dcn);
        paymentRepo.save(payment);

    }
    @PostMapping("/bulk-scan-payments")
    public void saveCasePayments( @RequestBody CaseDCNs caseDCNs){


        caseDCNs.getPayments().stream().forEach(payment -> casePaymentsRepo.save(payment));
    }
    @GetMapping("/bulk-scan-payments/{dcn}")
    public Optional<Payment> getPaymentByDCN(@PathVariable String dcn){

        return paymentRepo.findById(dcn);

    }
    @GetMapping("/bulk-scan-payments")
    public List<CaseDCN> getPaymentsForACase(@RequestParam String ccdCaseNumber){

        return casePaymentsRepo.findByCcdCaseNumberOrExceptionReference(ccdCaseNumber,ccdCaseNumber);

    }

}
