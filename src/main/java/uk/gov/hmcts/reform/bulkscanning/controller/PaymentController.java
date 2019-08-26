package uk.gov.hmcts.reform.bulkscanning.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDCNDto;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDCNs;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.service.PaymentService;

@RestController
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    private static ModelMapper modelMapper = new ModelMapper();

    @PutMapping("/bulk-scan-payments/{dcn}")
    public void savePayment(@PathVariable String dcn, @RequestBody PaymentDto payment){

        payment.setDcnPayment(dcn);
        paymentService.save(payment);

    }
    @PostMapping("/bulk-scan-payments")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveCasePayments( @RequestBody CaseDCNs caseDCNs){

        caseDCNs.getCasePayments().stream().forEach(payment -> paymentService.save(payment));
    }

    @PutMapping("/bulk-scan-payments")
    public void saveCasePayments( @RequestParam String exceptionReference,  @RequestBody CaseDCNDto caseDCNs){

        paymentService.updateCaseDCNs(caseDCNs, exceptionReference);
    }

    @GetMapping("/bulk-scan-payments/{dcn}")
    public PaymentDto getPaymentByDCN(@PathVariable String dcn){

         return paymentService.getPayment(dcn);

    }
    @GetMapping("/bulk-scan-payments")
    public List<CaseDCNDto> getPaymentsForACase(@RequestParam String ccdCaseNumber){

        return paymentService.getCasePayments(ccdCaseNumber);

    }


    public PaymentDto toPaymentDto() {
        return modelMapper.map(this, PaymentDto.class);
    }
}
