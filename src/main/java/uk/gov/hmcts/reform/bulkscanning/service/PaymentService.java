package uk.gov.hmcts.reform.bulkscanning.service;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.reform.bulkscanning.db.CasePaymentsRepo;
import uk.gov.hmcts.reform.bulkscanning.db.PaymentRepo;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDCNDto;
import uk.gov.hmcts.reform.bulkscanning.dto.CaseDCNs;
import uk.gov.hmcts.reform.bulkscanning.dto.PaymentDto;
import uk.gov.hmcts.reform.bulkscanning.model.CaseDCN;
import uk.gov.hmcts.reform.bulkscanning.model.Payment;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private CasePaymentsRepo casePaymentsRepo;

    public PaymentDto getPayment(String dcn) {
        return toPaymentDto(
            paymentRepo.findById(dcn)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DCN not found")));

    }

    public List<CaseDCNDto> getCasePayments(String ccdCaseNumber) {

        List<CaseDCN> caseDCNs = casePaymentsRepo.findByCcdCaseNumberOrExceptionReference(ccdCaseNumber, ccdCaseNumber);

        return caseDCNs.stream().map(caseDCN -> toCaseDCNDTO(caseDCN)).collect(Collectors.toList());

    }

    private static ModelMapper modelMapper = new ModelMapper();
    private CaseDCNDto toCaseDCNDTO(CaseDCN caseDCN) {
        return modelMapper.map(caseDCN, CaseDCNDto.class);
    }



    private PaymentDto toPaymentDto(Payment payment) {
        return modelMapper.map(payment, PaymentDto.class);
    }

    public void save(PaymentDto payment) {
        paymentRepo.save(toPayment(payment));

    }

    private Payment toPayment(PaymentDto payment) {
        return modelMapper.map(payment,Payment.class);
    }

    public void save(CaseDCN casePayment) {
        casePaymentsRepo.save(casePayment);
    }

    public void updateCaseDCNs(CaseDCNDto caseDCNDto, String exceptionReference) {
        List<CaseDCN> caseDCNs = casePaymentsRepo.findByCcdCaseNumberOrExceptionReference(exceptionReference, exceptionReference);

        caseDCNs.forEach(caseDCN -> caseDCN.setCcdCaseNumber(caseDCNDto.getCcdCaseNumber()));

    }
}
