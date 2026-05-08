package com.payflow.coreservice.repository;

import com.payflow.coreservice.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StatusHistoryRepository  extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findSource(String source);
}
