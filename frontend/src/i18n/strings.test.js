import { describe, expect, it } from 'vitest';
import { strings, LANGS } from './strings';

describe('i18n strings', () => {
  const enKeys = Object.keys(strings[LANGS.EN]).sort();
  const hiKeys = Object.keys(strings[LANGS.HI]).sort();

  it('has the same keys in English and Hindi', () => {
    expect(hiKeys).toEqual(enKeys);
  });

  it('defines every key for each language non-empty', () => {
    for (const lang of Object.values(LANGS)) {
      for (const key of enKeys) {
        expect(strings[lang][key], `${lang}.${key}`).toBeTruthy();
      }
    }
  });

  it('has both languages registered as values', () => {
    expect(Object.values(LANGS)).toEqual(['en', 'hi']);
  });
});