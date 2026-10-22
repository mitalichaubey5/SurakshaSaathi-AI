package com.surakshasaathi.ai.repository;

import com.surakshasaathi.ai.model.AlertLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    /**
     * Correlates a verified Meta delivery-status webhook back to the alert row
     * created when the message was accepted.
     */
    Optional<AlertLog> findByProviderMessageId(String providerMessageId);
}
