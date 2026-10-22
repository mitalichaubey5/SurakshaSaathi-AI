package com.surakshasaathi.ai.service.impl;

import com.surakshasaathi.ai.service.OcrService;
import com.surakshasaathi.ai.service.ocr.OcrConfigException;
import com.surakshasaathi.ai.service.ocr.OcrProcessingException;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Real Tess4J-based OCR implementation for screenshots.
 * - Validates content type, size, emptiness before processing
 * - Uses temporary file for image processing (never persists permanently)
 * - Cleans up resources
 * - Never logs OCR text or sensitive content
 * - Fails safely if Tesseract not configured
 */
@Service
public class OcrServiceImpl implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceImpl.class);
    
    private static final Set<String> ACCEPTED_IMAGE_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );
    
    private static final Set<String> ACCEPTED_EXTENSIONS = Set.of(
            "png",
            "jpg",
            "jpeg",
            "webp"
    );
    
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB

    @Value("${app.ocr.tessdata-path:}")
    private String tessDataPath;

    @Value("${app.ocr.default-language:eng}")
    private String defaultLanguage;

    @Override
    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please choose a screenshot to check.");
        }

        validateImage(file);

        Path tempFile = null;
        try {
            tempFile = createTempFile(file);
            BufferedImage image = ImageIO.read(tempFile.toFile());
            if (image == null) {
                throw new OcrProcessingException("We couldn't read this image. Please upload a clearer screenshot.");
            }

            ITesseract tesseract = createTesseract();
            String ocrText = tesseract.doOCR(image);
            
            if (!StringUtils.hasText(ocrText)) {
                throw new OcrProcessingException("No readable text was found in this image. Please try uploading a clearer screenshot.");
            }
            
            // Return trimmed text; do not log the actual content
            return ocrText.trim();
        } catch (OcrProcessingException | OcrConfigException e) {
            throw e;
        } catch (TesseractException e) {
            log.warn("OCR processing failed: {}", e.getMessage());
            throw new OcrProcessingException("We couldn't read this image. Please upload a clearer screenshot.");
        } catch (IOException e) {
            log.warn("Failed to process image file: {}", e.getMessage());
            throw new OcrProcessingException("This image format isn't supported or couldn't be processed.");
        } catch (Exception e) {
            log.warn("Unexpected OCR error: {}", e.getMessage());
            throw new OcrProcessingException("We couldn't read this image. Please try again.");
        } finally {
            cleanupTempFile(tempFile);
        }
    }

    private void validateImage(MultipartFile file) {
        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ACCEPTED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("This image format isn't supported. Please choose a PNG, JPG or WebP picture.");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("The image is too large. Please choose one below 5 MB.");
        }

        // Validate file name/extension to prevent issues
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.hasText(originalFilename)) {
            String extension = getFileExtension(originalFilename).toLowerCase(Locale.ROOT);
            if (!ACCEPTED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("This image format isn't supported. Please choose a PNG, JPG or WebP picture.");
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private Path createTempFile(MultipartFile file) throws IOException {
        String extension = StringUtils.hasText(file.getOriginalFilename()) 
            ? getFileExtension(file.getOriginalFilename()) 
            : "png";
        Path tempPath = Files.createTempFile("suraksha-ocr-", "." + extension);
        Files.write(tempPath, file.getBytes(), StandardOpenOption.WRITE);
        return tempPath;
    }

    private ITesseract createTesseract() {
        ITesseract tesseract = new Tesseract();
        String lang = (defaultLanguage != null && !defaultLanguage.isBlank()) ? defaultLanguage : "eng";
        tesseract.setLanguage(lang);

        // Set tessdata path if configured
        if (StringUtils.hasText(tessDataPath)) {
            Path tessDataDir = Paths.get(tessDataPath);
            if (!Files.exists(tessDataDir) || !Files.isDirectory(tessDataDir)) {
                throw new OcrConfigException("Tesseract tessdata directory not found. Please configure app.ocr.tessdata-path correctly.");
            }
            tesseract.setDatapath(tessDataPath);
        }

        try {
            // Test initialization by checking if language data exists
            tesseract.doOCR(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));
        } catch (TesseractException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("tessdata") || msg.contains("traineddata") || msg.contains("language"))) {
                throw new OcrConfigException("Tesseract language data missing or misconfigured for '" + lang + "'. Please ensure '" + lang + ".traineddata' exists in tessdata directory.");
            }
            throw new OcrConfigException("Tesseract OCR not properly configured: " + msg);
        }

        return tesseract;
    }

    private void cleanupTempFile(Path tempFile) {
        if (tempFile != null && Files.exists(tempFile)) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to clean up temporary OCR file: {}", e.getMessage());
            }
        }
    }
}
