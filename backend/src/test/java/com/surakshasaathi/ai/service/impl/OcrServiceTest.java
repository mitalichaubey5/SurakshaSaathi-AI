package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.service.ocr.OcrProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OcrServiceTest {

    private final OcrServiceImpl ocrService = new OcrServiceImpl();

    @Test
    void emptyFileThrowsIllegalArgument() {
        MockMultipartFile empty = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        assertThatThrownBy(() -> ocrService.extractText(empty))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullFileThrowsIllegalArgument() {
        assertThatThrownBy(() -> ocrService.extractText(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidContentTypeThrows() {
        MockMultipartFile invalid = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> ocrService.extractText(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oversizedFileThrows() {
        byte[] large = new byte[6 * 1024 * 1024];
        MockMultipartFile oversized = new MockMultipartFile("file", "big.png", "image/png", large);
        assertThatThrownBy(() -> ocrService.extractText(oversized))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsupportedExtensionThrows() {
        MockMultipartFile invalid = new MockMultipartFile("file", "test.gif", "image/png", "fake".getBytes());
        assertThatThrownBy(() -> ocrService.extractText(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nonReadableImageThrowsOcrProcessingException() {
        byte[] tinyPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                (byte) 0x08, (byte) 0x06, 0x00, 0x00, 0x00, (byte) 0x1F, (byte) 0x15, (byte) 0xC4, (byte) 0x89,
                0x00, 0x00, 0x00, 0x0B, 0x49, 0x44, 0x41, 0x54,
                (byte) 0x08, (byte) 0xD7, 0x00, 0x01, 0x00, 0x00, (byte) 0x05, 0x00, (byte) 0x01, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, (byte) 0x42, (byte) 0x60, (byte) 0x82
        };
        MockMultipartFile fake = new MockMultipartFile("file", "tiny.png", "image/png", tinyPng);
        assertThatThrownBy(() -> ocrService.extractText(fake))
                .isInstanceOf(OcrProcessingException.class);
    }
}
