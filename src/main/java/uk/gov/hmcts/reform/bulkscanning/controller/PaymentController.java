package uk.gov.hmcts.reform.bulkscanning.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanning.db.PaymentRepo;
import uk.gov.hmcts.reform.bulkscanning.model.Payment;

@RestController
public class PaymentController {

    @Autowired
    private PaymentRepo repo;
    @PutMapping("/bulk-scan-payments/{dcn}")
    public void savePayment(@PathVariable String dcn, @RequestBody Payment payment){

        payment.setDcnPayment(dcn);
        repo.save(payment);

    }
}
