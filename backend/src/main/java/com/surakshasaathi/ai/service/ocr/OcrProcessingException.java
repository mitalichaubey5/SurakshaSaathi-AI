package com.surakshasaathi.ai.service.ocr;

/**
 * Thrown when OCR processing fails due to image content, OCR runtime errors
 * or when no readable text is extracted. Messages stay safe for users.
 */
public class OcrProcessingException extends RuntimeException {
    public OcrProcessingException(String message) {
        super(message);
    }

    public OcrProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
