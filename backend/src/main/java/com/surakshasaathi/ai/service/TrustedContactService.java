package com.surakshasaathi.ai.service;

import com.surakshasaathi.ai.dto.trusted.CreateTrustedContactRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedContactResponse;
import com.surakshasaathi.ai.dto.trusted.UpdateTrustedContactRequest;

import java.util.List;

public interface TrustedContactService {
    TrustedContactResponse create(CreateTrustedContactRequest request);
    List<TrustedContactResponse> listAll();
    TrustedContactResponse getById(Long id);
    TrustedContactResponse update(Long id, UpdateTrustedContactRequest request);
    void delete(Long id);
}
