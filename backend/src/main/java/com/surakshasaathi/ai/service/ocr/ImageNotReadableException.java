package com.surakshasaathi.ai.service.ocr;

/**
 * Thrown when an image is not readable or has no extractable text.
 */
public class ImageNotReadableException extends RuntimeException {
    public ImageNotReadableException(String message) {
        super(message);
    }
}
