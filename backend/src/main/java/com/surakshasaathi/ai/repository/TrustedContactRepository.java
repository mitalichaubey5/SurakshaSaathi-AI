package com.surakshasaathi.ai.repository;

import com.surakshasaathi.ai.model.TrustedContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrustedContactRepository extends JpaRepository<TrustedContact, Long> {

    List<TrustedContact> findByEnabledTrue();
    List<TrustedContact> findAllByUserId(Long userId);
    Optional<TrustedContact> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
