package com.surakshasaathi.ai.service;

import com.surakshasaathi.ai.dto.analysis.AnalysisResponse;

/**
 * Core analysis pipeline contract (implemented in Step 2).
 *
 * Flow: Detect (rule engine) -> Explain (LLM) -> Verify -> Guide -> Escalate.
 */
public interface ScamAnalyzerService {

    /** Analyse a raw text message or URL. */
    AnalysisResponse analyzeText(String text, String language);

    /** Analyse OCR-extracted text from a screenshot. */
    AnalysisResponse analyzeOcrText(String ocrText, String language, String originalOcrText);
}
