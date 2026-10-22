# SurakshaSaathi AI - OCR Implementation (Step 5)

## Overview

Real OCR is implemented using **Tess4J** (Java wrapper for Tesseract OCR) to extract text from uploaded screenshots. The OCR service integrates seamlessly with the existing scam analysis pipeline without duplicating risk logic.

## Architecture

- **OcrService**: Interface contract (`service/OcrService.java`)
- **OcrServiceImpl**: Tess4J-based implementation with validation, temp file handling, cleanup, and privacy safeguards (`service/impl/OcrServiceImpl.java`)
- **Exceptions**: `OcrProcessingException`, `OcrConfigException`, `ImageNotReadableException` in `service/ocr/` package
- **Endpoint**: `POST /api/v1/analyze/image` (multipart) continues to work as before

## Dependencies

Tess4J is already included in `pom.xml`:
```xml
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.8.0</version>
</dependency>
```

## Configuration

Configure in `application.yml` (`backend/src/main/resources/application.yml`):

```yaml
app:
  ocr:
    tessdata-path: ${TESSDATA_PREFIX:C:/Program Files/Tesseract-OCR/tessdata}  # Optional
    default-language: eng  # Default language (eng, hin supported if tessdata available)
```

Environment variables:
- `TESSDATA_PREFIX` or set `app.ocr.tessdata-path` explicitly to point to Tesseract's `tessdata` directory
- No hardcoded personal paths are required

## Prerequisites (Environment Setup)

To perform real OCR on the current/target machine, you need:

1. **Install Tesseract OCR** (native binary)
   - Windows: [UB Mannheim build](https://github.com/UB-Mannheim/tesseract/wiki) or [official builds](https://tesseract-ocr.github.io/)
   - Linux: `apt-get install tesseract-ocr`
   - macOS: `brew install tesseract`

2. **Install language data (traineddata files)**
   - Place `eng.traineddata` in the `tessdata/` directory (minimum required for English)
   - For Hindi, place `hin.traineddata` in the same directory
   - Default location examples:
     - Windows: `C:/Program Files/Tesseract-OCR/tessdata/` or `C:/Program Files (x86)/Tesseract-OCR/tessdata/`
     - Linux: `/usr/share/tesseract-ocr/4.00/tessdata/` or `/usr/share/tessdata/`

3. **Configure the application**
   - Set `app.ocr.tessdata-path` to your tessdata directory, OR
   - Set environment variable `TESSDATA_PREFIX` pointing to the Tesseract installation root

If Tesseract is not installed or language data is missing, the application will fail safely with a clear configuration error (returned as user-friendly message, internal details not exposed to frontend).

## Privacy & Security

- **No permanent storage**: Uploaded images are written to a temporary file, processed, then deleted. Screenshots are never persisted.
- **No content logging**: OCR text, OTPs, PINs, card numbers, or other sensitive content are never logged.
- **Input validation**: Strict validation of content-type, file extension, file size (max 5MB), emptiness. Filenames are not trusted.
- **Temp file safety**: Temporary files use `suraksha-ocr-` prefix with proper cleanup in `finally` block.
- **Path safety**: No user-controlled paths are written; temp files are created via `Files.createTempFile()`.
- **Untrusted input**: Invalid/corrupted images are rejected with safe messages.

## Supported Formats

- PNG (`image/png`)
- JPEG/JPG (`image/jpeg`, `image/jpg`) 
- WebP (`image/webp`)
- Max size: 5MB (consistent with `spring.servlet.multipart.max-file-size: 5MB`)

## Error Handling

| Case | Response | Frontend display |
|---|---|---|
| Empty file / null | 400 "Please choose a screenshot to check." | Client-side validation also applies |
| Invalid content-type/extension | 400 "This image format isn't supported. Please choose a PNG, JPG or WebP picture." | i18n message (`fileTypeError`) |
| File too large (>5MB) | 400 "The image is too large. Please choose one below 5 MB." | i18n message (`fileSizeError`) |
| Unreadable/corrupted image | 400 "We couldn't read this image. Please upload a clearer screenshot." | Safe, elderly-friendly |
| No readable text found | 400 "No readable text was found in this image. Please try uploading a clearer screenshot." | Helpful guidance |
| Missing Tesseract/tessdata | 500 "Something went wrong. Please try again." (internal details logged, not exposed) | Safe fallback |
| OCR processing failure | 400 "We couldn't read this image. Please upload a clearer screenshot." | Safe, non-technical |

## Pipeline Integration

```
Image upload (multipart "file") + language param
    ↓
OcrServiceImpl.validateImage() (type, size, extension)
    ↓
Create temp file → ImageIO.read() → Tess4J OCR → extract text
    ↓
ScamAnalyzerService.analyzeOcrText(ocrText, language, ocrText)
    ↓
Rule engine + LLM (existing logic, unchanged)
    ↓
Structured AnalysisResponse (inputType="IMAGE", ocrText stored in response as per DTO)
```

**Note**: The scam analysis logic is not duplicated — OCR is purely text extraction. All existing risk merging, fallbacks, and i18n behavior remain unchanged.

## Testing

**Backend unit tests** (`OcrServiceTest`, 6 tests):
- Empty/null file validation
- Invalid content-type
- Oversized file (>5MB)
- Unsupported extension (e.g., .gif)
- Non-readable/corrupted image handling

**All existing tests continue to pass**: 27 original + 6 new OCR tests = 33 backend tests total. Frontend tests remain 19/19.

**Regression tests covered**: text analysis, emergency guidance (en/hi), trusted-circle alert, image validation errors.

## Hindi OCR (Optional)

To enable Hindi OCR:
1. Install `hin.traineddata` in tessdata directory
2. Configure `app.ocr.default-language: hin` (or pass language from frontend if desired)
3. The OCR service reads the configured language; controller passes language param but OCR service currently uses default config — this is a clean extension point documented for future enhancement

## Current Machine Status

- **Tesseract runtime**: Not detected on this machine (no `tesseract.exe` in PATH; tessdata not found)
- **OCR implementation**: Complete, production-ready code with proper error handling, validation, privacy, and cleanup
- **Real OCR smoke test**: Not performed due to missing native runtime. Tests that require real OCR are not added as they would be environment-dependent.
- **Behavior when Tesseract missing**: Service will throw `OcrConfigException` → mapped to safe 500 user message (no internal paths exposed). This is correct per requirements.

## Files Changed

**New files:**
- `backend/src/main/java/com/surakshasaathi/ai/service/ocr/OcrProcessingException.java`
- `backend/src/main/java/com/surakshasaathi/ai/service/ocr/OcrConfigException.java`
- `backend/src/main/java/com/surakshasaathi/ai/service/ocr/ImageNotReadableException.java`
- `backend/src/test/java/com/surakshasaathi/ai/service/impl/OcrServiceTest.java`
- `backend/OCR_README.md` (this file)

**Modified files:**
- `backend/src/main/java/com/surakshasaathi/ai/service/impl/OcrServiceImpl.java` (replaced placeholder with real Tess4J implementation)
- `backend/src/main/java/com/surakshasaathi/ai/config/GlobalExceptionHandler.java` (added handlers for OCR exceptions)

**Unchanged (as required):** All controllers, DTOs, scam analyzer logic, rule engine, LLM service, frontend code, i18n, existing tests.

## Build & Test Results

```bash
# Backend
mvn test → BUILD SUCCESS (33 tests passed: 5+12+10+6)
mvn package → BUILD SUCCESS (executable JAR created)

# Frontend  
npm test → 19/19 passed
npm run build → production build successful
```

All existing functionality verified via smoke tests (text analysis, emergency guidance en/hi, trusted alert, image validation errors).
