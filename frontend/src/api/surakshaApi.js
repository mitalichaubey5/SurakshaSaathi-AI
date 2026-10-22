import { getJson, postJson, postMultipart } from './http';

/**
 * API calls for SurakshaSaathi, matching the Spring Boot endpoints:
 *
 *   POST /api/v1/analyze/text         { text, language }
 *   POST /api/v1/analyze/image        multipart file + language query param
 *   POST /api/v1/trusted-circle/alert { contactName, contactPhone, suspiciousContent, riskLevel, language }
 *   GET  /api/v1/emergency/guideline  ?language=en|hi
 */

export function analyzeText(text, language) {
  return postJson('/api/v1/analyze/text', { text, language });
}

export function analyzeImage(file, language) {
  const formData = new FormData();
  formData.append('file', file);
  return postMultipart('/api/v1/analyze/image', formData, `language=${encodeURIComponent(language)}`);
}

export function sendTrustedAlert({ contactName, contactPhone, suspiciousContent, riskLevel, language }) {
  return postJson('/api/v1/trusted-circle/alert', {
    contactName,
    contactPhone,
    suspiciousContent,
    riskLevel,
    language,
  });
}

/** Returns the official emergency helpline guidance (1930 / 112 / cybercrime.gov.in). */
export function getEmergencyGuideline(language) {
  return getJson(`/api/v1/emergency/guideline?language=${encodeURIComponent(language)}`);
}