package com.payflow.coreservice.strategy.handlers;

import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import com.payflow.coreservice.strategy.PaymentStatusHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ApprovedHandler implements PaymentStatusHandler {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void handle(Payment payment, FraudAnalysisResponse response) {
        try{
            User payer = userRepository.findByUuid(payment.getPayerId())
                    .orElseThrow(() -> new RuntimeException("Payer não encontrado"));
            User payee = userRepository.findByUuid(payment.getPayeeId())
                    .orElseThrow(() -> new RuntimeException("Payee não encontrado"));

            if(payer.getBalance().compareTo(payment.getAmount()) < 0){
                payment.setStatus(Enum_Payment.FAILED);
                paymentRepository.save(payment);
                throw new RuntimeException("Saldo insuficiente");
            }

            BigDecimal amount = payment.getAmount();

            payer.setBalance(payer.getBalance().subtract(amount));
            userRepository.save(payer);

            payee.setBalance(payee.getBalance().add(amount));
            userRepository.save(payee);

            payment.setStatus(Enum_Payment.SUCCESS);
            paymentRepository.save(payment);

        } catch (Exception e) {
            payment.setStatus(Enum_Payment.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Erro ao processar pagamento: " + e.getMessage(), e);
        }

    }
}
