package com.payflow.coreservice.repository;

import com.payflow.coreservice.model.User;
import feign.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


@Repository
public interface UserRepository extends JpaRepository <User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByDocument(String document);

    Optional<User> findByUuid(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.uuid = :uuid")
    Optional<User> findByUuidForUpdate(
            @Param("uuid") UUID uuid
    );
}

