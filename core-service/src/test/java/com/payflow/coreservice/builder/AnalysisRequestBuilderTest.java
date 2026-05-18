package com.payflow.coreservice.builder;

import com.payflow.commons.dto.fraud.FraudAnalysisRequest;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalysisRequestBuilder")
class AnalysisRequestBuilderTest {

    @Test
    @DisplayName("deve mapear todos os campos do Payment para FraudAnalysisRequest")
    void shouldMapAllFields() {
        Payment payment = TestFixtures.pendingPayment();

        FraudAnalysisRequest result = AnalysisRequestBuilder.fromAnalysisRequest(payment);

        assertThat(result.getPaymentId()).isEqualTo(payment.getUuid());
        assertThat(result.getPayerId()).isEqualTo(payment.getPayerId());
        assertThat(result.getPayeeId()).isEqualTo(payment.getPayeeId());
        assertThat(result.getAmount()).isEqualByComparingTo(payment.getAmount());
    }

    @Test
    @DisplayName("não deve retornar null")
    void shouldNeverReturnNull() {
        Payment payment = TestFixtures.pendingPayment();

        FraudAnalysisRequest result = AnalysisRequestBuilder.fromAnalysisRequest(payment);

        assertThat(result).isNotNull();
    }
}
