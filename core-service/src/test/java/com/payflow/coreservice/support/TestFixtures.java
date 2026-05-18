package com.payflow.coreservice.support;

import com.payflow.commons.dto.alert.ManualReviewDecision;
import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.commons.dto.fraud.FraudAnalysisResponse;
import com.payflow.commons.dto.payment.PaymentRequest;
import com.payflow.commons.enums.fraud.Status_Fraud;
import com.payflow.commons.enums.payment.Enum_Payment;
import com.payflow.commons.enums.user.User_Status;
import com.payflow.coreservice.dto.RegisterRequestDTO;
import com.payflow.coreservice.enums.Document_Type;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TestFixtures {

    private TestFixtures() {}

    public static final UUID PAYER_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    public static final UUID PAYEE_ID  = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    public static final UUID PAYMENT_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000003");

    // ─── Payment ────────────────────────────────────────────────────────────

    public static Payment pendingPayment() {
        return Payment.builder()
                .uuid(PAYMENT_ID)
                .payerId(PAYER_ID)
                .payeeId(PAYEE_ID)
                .amount(new BigDecimal("500.00"))
                .status(Enum_Payment.PENDING)
                .idempotencyKey("idem-key-001")
                .createdAt(LocalDateTime.of(2026, 5, 18, 12, 0))
                .build();
    }

    public static PaymentRequest paymentRequest() {
        return PaymentRequest.builder()
                .payerId(PAYER_ID)
                .payeeId(PAYEE_ID)
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("idem-key-001")
                .build();
    }

    // ─── Users ──────────────────────────────────────────────────────────────

    public static User activeUser(UUID id, BigDecimal balance) {
        return User.builder()
                .uuid(id)
                .name("Test User")
                .email("user@test.com")
                .password("encodedPass")
                .document("123.456.789-00")
                .documentType("CPF")
                .balance(balance)
                .status(User_Status.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static RegisterRequestDTO registerRequest() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setName("João Silva");
        dto.setEmail("joao@email.com");
        dto.setPassword("senha123");
        dto.setConfirmPassword("senha123");
        dto.setDocument("123.456.789-00");
        dto.setDocumentType(Document_Type.CPF);
        dto.setBalance(new BigDecimal("1000.00"));
        return dto;
    }

    // ─── Fraud ──────────────────────────────────────────────────────────────

    public static FraudAnalysisResponse fraudResponse(Status_Fraud status) {
        return FraudAnalysisResponse.builder()
                .paymentId(PAYMENT_ID)
                .status(status)
                .score(status == Status_Fraud.APPROVED ? 0.0 : 50.0)
                .reason("Motivo de teste")
                .build();
    }

    // ─── Manual Review ───────────────────────────────────────────────────────

    public static ManualReviewDecision approvedDecision() {
        return ManualReviewDecision.builder()
                .paymentId(PAYMENT_ID)
                .reviewerId("analista-01")
                .decision("APPROVED")
                .reason("Cliente confirmou a operação")
                .notes("Verificado por telefone")
                .build();
    }

    public static ManualReviewDecision rejectedDecision() {
        return ManualReviewDecision.builder()
                .paymentId(PAYMENT_ID)
                .reviewerId("analista-01")
                .decision("REJECTED")
                .reason("Operação suspeita confirmada")
                .notes("")
                .build();
    }
}
