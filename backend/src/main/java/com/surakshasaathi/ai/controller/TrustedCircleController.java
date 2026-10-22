package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.dto.ApiResponse;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertResponse;
import com.surakshasaathi.ai.service.TrustedCircleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Escalation step — "Escalate": notify a trusted family member.
 *
 * POST /api/v1/trusted-circle/alert
 */
@RestController
@RequestMapping("/api/v1/trusted-circle")
public class TrustedCircleController {

    private final TrustedCircleService trustedCircleService;

    public TrustedCircleController(TrustedCircleService trustedCircleService) {
        this.trustedCircleService = trustedCircleService;
    }

    @PostMapping("/alert")
    public ApiResponse<TrustedAlertResponse> sendAlert(@Valid @RequestBody TrustedAlertRequest request) {
        TrustedAlertResponse response = trustedCircleService.sendAlert(request);
        return ApiResponse.ok("Trusted person notified", response);
    }
}
