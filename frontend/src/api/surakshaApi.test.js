import { afterEach, describe, expect, it, vi } from 'vitest';
import { analyzeText, analyzeImage, sendTrustedAlert, getEmergencyGuideline } from './surakshaApi';

const okEnvelope = (data, message) =>
  JSON.stringify({ success: true, message: message || 'ok', data, timestamp: '2026-10-08T00:00:00Z' });

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('surakshaApi', () => {
  it('analyzeText posts { text, language } to /api/v1/analyze/text', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(okEnvelope({ riskLevel: 'HIGH' }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const res = await analyzeText('Your KYC has expired', 'hi');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/analyze/text');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ text: 'Your KYC has expired', language: 'hi' });
    expect(res.data.riskLevel).toBe('HIGH');
  });

  it('analyzeImage sends a multipart FormData file with language query', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(okEnvelope({ riskLevel: 'MEDIUM', inputType: 'IMAGE' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const fakeFile = new File(['x'], 'shot.png', { type: 'image/png' });
    const res = await analyzeImage(fakeFile, 'en');

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toContain('/api/v1/analyze/image');
    expect(url).toContain('language=en');
    expect(options.body instanceof FormData).toBe(true);
    expect(options.body.get('file').name).toBe('shot.png');
    expect(res.data.inputType).toBe('IMAGE');
  });

  it('sendTrustedAlert posts the trusted-circle payload', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(okEnvelope({ delivered: true, confirmationMessage: 'sent' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    const res = await sendTrustedAlert({
      contactName: 'Ravi',
      contactPhone: '9876543210',
      suspiciousContent: 'hi',
      riskLevel: 'HIGH',
      language: 'en',
    });

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/trusted-circle/alert');
    const body = JSON.parse(options.body);
    expect(body.contactName).toBe('Ravi');
    expect(body.riskLevel).toBe('HIGH');
    expect(res.data.delivered).toBe(true);
  });

  it('maps a 400 validation response onto a VALIDATION error with the backend message', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: false, message: 'Please paste a message.', data: null }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    vi.stubGlobal('fetch', fetchMock);

    await expect(analyzeText('', 'en')).rejects.toMatchObject({
      code: 'VALIDATION',
      message: 'Please paste a message.',
      status: 400,
    });
  });

  it('getEmergencyGuideline GETs the official guidance with language', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        okEnvelope({ headline: 'help', helplines: [{ name: '1930', number: '1930', whenToUse: 'x' }], reportUrl: 'https://cybercrime.gov.in' }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    );
    vi.stubGlobal('fetch', fetchMock);

    const res = await getEmergencyGuideline('hi');

    const [url] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/v1/emergency/guideline?language=hi');
    expect(fetchMock.mock.calls[0].length).toBe(1); // plain GET, no options
    expect(res.data.helplines[0].number).toBe('1930');
  });

  it('maps a network failure onto a NETWORK error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(analyzeText('hello', 'en')).rejects.toMatchObject({ code: 'NETWORK' });
  });
});