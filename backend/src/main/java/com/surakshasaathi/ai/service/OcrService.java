package com.surakshasaathi.ai.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * OCR contract (Tesseract via Tess4J) — implemented in Step 2.
 */
public interface OcrService {

    /** Extract plain text from an uploaded screenshot. */
    String extractText(MultipartFile file);
}
