package com.surakshasaathi.ai.service;

import com.surakshasaathi.ai.dto.trusted.TrustedAlertRequest;
import com.surakshasaathi.ai.dto.trusted.TrustedAlertResponse;

/** Trusted-circle escalation contract (mock notification for the prototype). */
public interface TrustedCircleService {

    TrustedAlertResponse sendAlert(TrustedAlertRequest request);
}
