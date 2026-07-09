import { createI18n } from 'vue-i18n';
import type { WritableComputedRef } from 'vue';
import lv from './lv';
import en from './en';
import ru from './ru';

export type MessageSchema = typeof lv;

const i18n = createI18n<[MessageSchema], 'lv' | 'en' | 'ru'>({
  legacy: false,
  locale: 'lv',
  fallbackLocale: 'en',
  messages: { lv, en, ru },
});

export default i18n;
export type Locale = 'lv' | 'en' | 'ru';
export const LOCALES: Locale[] = ['lv', 'en', 'ru'];

const LOCALE_STORAGE_KEY = 'nami:locale';

export function setLocale(locale: Locale): void {
  (i18n.global.locale as unknown as WritableComputedRef<Locale>).value = locale;
  try {
    localStorage.setItem(LOCALE_STORAGE_KEY, locale);
  } catch {
    // Persistence is best-effort; ignore private-mode / quota errors.
  }
}

function storedLocale(): Locale | null {
  try {
    const raw = localStorage.getItem(LOCALE_STORAGE_KEY);
    return (LOCALES as string[]).includes(raw ?? '') ? (raw as Locale) : null;
  } catch {
    return null;
  }
}

/**
 * Best-effort locale for a visitor who lands without a locale in the URL.
 * Prefers a previously chosen locale, then the browser's preferred languages,
 * falling back to Latvian.
 */
export function detectBrowserLocale(): Locale {
  const stored = storedLocale();
  if (stored) return stored;

  const candidates = navigator.languages?.length
    ? navigator.languages
    : [navigator.language];
  for (const lang of candidates) {
    const code = lang.slice(0, 2).toLowerCase();
    if ((LOCALES as string[]).includes(code)) return code as Locale;
  }
  return 'lv';
}
