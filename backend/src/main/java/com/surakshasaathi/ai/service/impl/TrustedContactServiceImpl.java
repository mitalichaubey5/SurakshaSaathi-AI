package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.dto.trusted.CreateTrustedContactRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedContactResponse;
import com.surakshasaathi.ai.dto.trusted.UpdateTrustedContactRequest;
import com.surakshasaathi.ai.model.TrustedContact;
import com.surakshasaathi.ai.model.User;
import com.surakshasaathi.ai.repository.TrustedContactRepository;
import com.surakshasaathi.ai.repository.UserRepository;
import com.surakshasaathi.ai.service.TrustedContactService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TrustedContactServiceImpl implements TrustedContactService {
    private final TrustedContactRepository repository;
    private final UserRepository userRepository;

    public TrustedContactServiceImpl(TrustedContactRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetails ud)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public TrustedContactResponse create(CreateTrustedContactRequest request) {
        if ((request.phoneNumber() == null || request.phoneNumber().isBlank()) &&
            (request.email() == null || request.email().isBlank())) {
            throw new IllegalArgumentException("At least one contact method (phone or email) is required");
        }
        User user = getCurrentUser();
        TrustedContact tc = new TrustedContact();
        tc.setName(request.name());
        tc.setPhone(request.phoneNumber());
        tc.setEmail(request.email());
        tc.setRelationship(request.relationship());
        tc.setPreferredLanguage(request.preferredLanguage() != null ? request.preferredLanguage() : "en");
        tc.setEnabled(true);
        tc.setUser(user);
        tc = repository.save(tc);
        return map(tc);
    }

    @Override
    public List<TrustedContactResponse> listAll() {
        User user = getCurrentUser();
        return repository.findAllByUserId(user.getId()).stream().map(this::map).toList();
    }

    @Override
    public TrustedContactResponse getById(Long id) {
        User user = getCurrentUser();
        TrustedContact tc = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Trusted contact not found"));
        return map(tc);
    }

    @Override
    public TrustedContactResponse update(Long id, UpdateTrustedContactRequest request) {
        User user = getCurrentUser();
        TrustedContact tc = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Trusted contact not found"));
        tc.setName(request.name());
        tc.setPhone(request.phoneNumber());
        tc.setEmail(request.email());
        tc.setRelationship(request.relationship());
        if (request.preferredLanguage() != null) {
            tc.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.enabled() != null) {
            tc.setEnabled(request.enabled());
        }
        tc = repository.save(tc);
        return map(tc);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getCurrentUser();
        if (repository.findByIdAndUserId(id, user.getId()).isEmpty()) {
            throw new IllegalArgumentException("Trusted contact not found");
        }
        repository.deleteByIdAndUserId(id, user.getId());
    }

    private TrustedContactResponse map(TrustedContact tc) {
        return new TrustedContactResponse(tc.getId(), tc.getName(), tc.getPhone(), tc.getEmail(),
                tc.getRelationship(), tc.getPreferredLanguage(), tc.isEnabled(), tc.getCreatedAt(), tc.getUpdatedAt());
    }
}
