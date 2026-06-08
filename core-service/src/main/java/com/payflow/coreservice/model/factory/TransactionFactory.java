package com.payflow.coreservice.model.factory;

import com.payflow.coreservice.enums.Enum_Transaction;
import com.payflow.coreservice.model.Payment;
import com.payflow.coreservice.model.Transaction;

import java.time.LocalDateTime;

public class TransactionFactory {

    private TransactionFactory(){

    }

    public static Transaction fromPayment(
            Payment payment,
            Enum_Transaction status,
            String reason
    ){
        Transaction transaction = new Transaction();
        transaction.setPaymentId(payment.getUuid());
        transaction.setStatus(status);
        transaction.setReason(reason);
        transaction.setPayeeId(payment.getPayeeId());
        transaction.setPayerId(payment.getPayerId());
        transaction.setExecutedAt(LocalDateTime.now());
        return transaction;
    }
}
