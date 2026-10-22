package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.dto.ApiResponse;
import com.surakshasaathi.ai.dto.trusted.CreateTrustedContactRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedContactResponse;
import com.surakshasaathi.ai.dto.trusted.UpdateTrustedContactRequest;
import com.surakshasaathi.ai.service.TrustedContactService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trusted-circle/contacts")
public class TrustedCircleContactController {
    private final TrustedContactService service;

    public TrustedCircleContactController(TrustedContactService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<TrustedContactResponse> create(@Valid @RequestBody CreateTrustedContactRequest request) {
        return ApiResponse.ok("Trusted contact created", service.create(request));
    }

    @GetMapping
    public ApiResponse<List<TrustedContactResponse>> list() {
        return ApiResponse.ok("Trusted contacts", service.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<TrustedContactResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Trusted contact", service.getById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<TrustedContactResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTrustedContactRequest request) {
        return ApiResponse.ok("Trusted contact updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Trusted contact deleted", null);
    }
}
