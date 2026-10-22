package com.surakshasaathi.ai.controller;

import com.surakshasaathi.ai.dto.ApiResponse;
import com.surakshasaathi.ai.dto.analysis.AnalysisResponse;
import com.surakshasaathi.ai.dto.analysis.TextAnalyzeRequest;
import com.surakshasaathi.ai.service.OcrService;
import com.surakshasaathi.ai.service.ScamAnalyzerService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Detection entry points — "Detect -> Explain -> Verify -> Guide".
 *
 * POST /api/v1/analyze/text   : suspicious message or URL
 * POST /api/v1/analyze/image  : screenshot (OCR -> same analysis pipeline)
 */
@RestController
@RequestMapping("/api/v1/analyze")
public class AnalyzeController {

    private final ScamAnalyzerService scamAnalyzerService;
    private final OcrService ocrService;

    public AnalyzeController(ScamAnalyzerService scamAnalyzerService, OcrService ocrService) {
        this.scamAnalyzerService = scamAnalyzerService;
        this.ocrService = ocrService;
    }

    /** Text / URL analysis. */
    @PostMapping("/text")
    public ApiResponse<AnalysisResponse> analyzeText(@Valid @RequestBody TextAnalyzeRequest request) {
        AnalysisResponse result = scamAnalyzerService.analyzeText(request.text(), request.safeLanguage());
        return ApiResponse.ok("Analysis complete", result);
    }

    /** Screenshot analysis: OCR first, then route the extracted text through the same engine. */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AnalysisResponse> analyzeImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "en") String language) {

        String extractedText = ocrService.extractText(file);
        AnalysisResponse result = scamAnalyzerService.analyzeOcrText(extractedText, language, extractedText);
        return ApiResponse.ok("Screenshot analysed", result);
    }
}
