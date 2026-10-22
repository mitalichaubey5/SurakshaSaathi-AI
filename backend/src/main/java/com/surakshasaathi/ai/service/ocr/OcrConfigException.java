package com.surakshasaathi.ai.service.ocr;

/**
 * Thrown when OCR is misconfigured (missing Tesseract, missing tessdata etc.).
 * Handled separately so we can return a clear configuration message.
 */
public class OcrConfigException extends RuntimeException {
    public OcrConfigException(String message) {
        super(message);
    }

    public OcrConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
