package com.payflow.coreservice.services;

import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.dto.factory.PaymentResponseFactory;
import com.payflow.coreservice.enums.Enum_Payment;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.factory.PaymentFactory;
import com.payflow.coreservice.model.User;
import com.payflow.coreservice.repository.PaymentRepository;
import com.payflow.coreservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Data
@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    // =========================
    // CREATE PAYMENT
    // =========================

    @Transactional
    public PaymentResponseDTO createPayment(PaymentRequestDTO dto) {

        // 1. Idempotência
        validateIdempotency(dto.getIdempotencyKey());

        // 2. Buscar usuários
        User payer = findUser(dto.getPayerId());
        User payee = findUser(dto.getPayeeId());

        // 3. Regras de negócio
        if (dto.getPayerId().equals(dto.getPayeeId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Transferência inválida");
        }

        if (payer.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Saldo insuficiente");
        }

        // 4. Fraud (simulado)
        authorizePayment();

        // 5. Atualizar saldo
        payer.setBalance(payer.getBalance().subtract(dto.getAmount()));
        payee.setBalance(payee.getBalance().add(dto.getAmount()));

        userRepository.save(payer);
        userRepository.save(payee);

        // 6. Criar pagamento
        Payment payment = PaymentFactory.fromRequest(dto, Enum_Payment.SUCCESS);

        Payment saved = paymentRepository.save(payment);

        return PaymentResponseFactory.fromPayment(saved);
    }

    // =========================
    // HELPERS
    // =========================

    private void validateIdempotency(String key) {
        if (paymentRepository.findByIdempotencyKey(key).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Pagamento já processado");
        }
    }

    private void authorizePayment() {
        boolean autorizado = true; // simulação

        if (!autorizado) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Pagamento não autorizado");
        }
    }

    private User findUser(UUID id) {
        return userRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public PaymentResponseDTO getById(UUID id) {
        Payment payment = paymentRepository.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        return PaymentResponseFactory.fromPayment(payment);
    }

    public List<PaymentResponseDTO> getByUser(UUID userId) {

        List<Payment> payments =
                paymentRepository.findByPayerIdOrPayeeId(userId, userId);

        return payments.stream()
                .map(PaymentResponseFactory::fromPayment)
                .toList();
    }
}