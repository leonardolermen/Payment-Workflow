package com.payflow.fraudservice.Repository;
import com.payflow.fraudservice.Enums.fraud.Status_Fraud;
import com.payflow.fraudservice.model.FraudAnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudLog_Repository extends JpaRepository<FraudAnalysisLog, Long> {

    List<FraudAnalysisLog> findByPaymentId(UUID paymentId);

    List<FraudAnalysisLog> findByStatus(Status_Fraud status);
}
