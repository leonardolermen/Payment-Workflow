package com.payflow.coreservice.services;

import com.payflow.coreservice.dto.PaymentRequestDTO;
import com.payflow.coreservice.dto.PaymentResponseDTO;
import com.payflow.coreservice.enums.Enum_Payment;
import com.payflow.coreservice.model.Payment;
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
        Payment payment = new Payment();
        payment.setPayerId(dto.getPayerId());
        payment.setPayeeId(dto.getPayeeId());
        payment.setAmount(dto.getAmount());
        payment.setStatus(Enum_Payment.SUCCESS);
        payment.setIdempotencyKey(dto.getIdempotencyKey());

        Payment saved = paymentRepository.save(payment);

        return toDTO(saved);
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

    private User findUser(java.util.UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private PaymentResponseDTO toDTO(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        return dto;
    }

    public PaymentResponseDTO getById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pagamento não encontrado"));

        return toDTO(payment);
    }

    public List<PaymentResponseDTO> getByUser(UUID userId) {

        List<Payment> payments =
                paymentRepository.findByPayerIdOrPayeeId(userId, userId);

        return payments.stream()
                .map(this::toDTO)
                .toList();
    }
}