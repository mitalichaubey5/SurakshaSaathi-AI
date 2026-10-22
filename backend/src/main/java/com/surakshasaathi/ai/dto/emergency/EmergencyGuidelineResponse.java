package com.surakshasaathi.ai.dto.emergency;

import java.util.List;

/**
 * Response for GET /api/v1/emergency/guideline
 * Indian cybercrime helpline guidance shown on the "Emergency Help" screen.
 */
public record EmergencyGuidelineResponse(
        String headline,
        List<Helpline> helplines,
        List<String> immediateSteps,
        String reportUrl,
        String slogan
) {

    public record Helpline(String name, String number, String whenToUse) { }
}
