package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.dto.ApiResponse;
import com.surakshasaathi.ai.dto.emergency.EmergencyGuidelineResponse;
import com.surakshasaathi.ai.dto.emergency.EmergencyGuidelineResponse.Helpline;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Escalation step — official Indian cybercrime guidance.
 *
 * GET /api/v1/emergency/guideline?language=en|hi
 * Static/official data served directly (no LLM needed — accuracy matters here).
 */
@RestController
@RequestMapping("/api/v1/emergency")
public class EmergencyController {

    @GetMapping("/guideline")
    public ApiResponse<EmergencyGuidelineResponse> guideline(
            @RequestParam(value = "language", defaultValue = "en") String language) {

        boolean hindi = "hi".equalsIgnoreCase(language);

        EmergencyGuidelineResponse response = new EmergencyGuidelineResponse(
                hindi
                        ? "Agar aapke saath dhokha hua hai — jaldi se ye karein"
                        : "If you have been cheated — do these right now",
                List.of(
                        new Helpline("National Cyber Crime Helpline", "1930",
                                hindi
                                        ? "Turant paisa wapas chahiye toh 1930 par call karein (24x7)"
                                        : "Call 1930 to report financial fraud and freeze the money (24x7)"),
                        new Helpline("Police Emergency", "112",
                                hindi
                                        ? "Khatre ya violence ki halat mein 112 call karein"
                                        : "Call 112 if you are in immediate danger"),
                        new Helpline("Women Helpline", "181",
                                hindi
                                        ? "Mahilaon ke liye madad: 181"
                                        : "Helpline for women: 181")
                ),
                List.of(
                        hindi ? "1. Koi bhi link mat click karein." : "1. Do NOT click any link.",
                        hindi ? "2. Kisi ko OTP, PIN ya password mat batayein." : "2. Do NOT share OTP, PIN or password.",
                        hindi ? "3. Bank ko turant call karke card block karein." : "3. Call your bank and block the card.",
                        hindi ? "4. 1930 par call karein ya cybercrime.gov.in par shikayat likhein."
                              : "4. Call 1930 or file a complaint at cybercrime.gov.in.",
                        hindi ? "5. Apne bharosemand vyakti ko batayein." : "5. Tell your trusted person."
                ),
                "https://cybercrime.gov.in",
                "Pause. Verify. Then Trust."
        );

        return ApiResponse.ok("Emergency guidance loaded", response);
    }
}
