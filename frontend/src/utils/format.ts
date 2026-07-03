import type { Locale } from '../i18n';
import { ListingType } from '../types/listingItem';

const INTL_LOCALE: Record<Locale, string> = {
  lv: 'lv-LV',
  en: 'en-IE',
  ru: 'ru-RU',
};

function currencyFmt(maximumFractionDigits: number) {
  const byLocale = {} as Record<Locale, Intl.NumberFormat>;
  for (const locale of Object.keys(INTL_LOCALE) as Locale[]) {
    byLocale[locale] = new Intl.NumberFormat(INTL_LOCALE[locale], {
      style: 'currency',
      currency: 'EUR',
      minimumFractionDigits: 0,
      maximumFractionDigits,
    });
  }
  return (locale: Locale) => byLocale[locale];
}

const fmt = currencyFmt(0);
const fmtPerM2 = currencyFmt(2);

// Grouped, no-decimal number formatting shared by price inputs/filters
// (e.g. "300,000") — deliberately locale-fixed to 'en-IE' rather than the
// UI locale, since ',' is used purely as a thousands separator here.
export const groupFmt = new Intl.NumberFormat('en-IE', {
  maximumFractionDigits: 0,
});

// format/parse pair for whole-euro price inputs (FormField's `format`/`parse`
// props): display grouped digits, store the raw digit string. Pair with a
// numericInput beforeinput guard so stray separators never get typed.
export function formatPriceInput(value: string): string {
  return value ? groupFmt.format(Number(value)) : '';
}

export function parsePriceInput(raw: string): string {
  return raw.replace(/\D/g, '');
}

export function formatPrice(
  value: number,
  type: ListingType,
  locale: Locale = 'lv'
): string {
  const formatted = fmt(locale).format(value);
  if (type !== 'rent') return formatted;
  const suffix =
    locale === 'en' ? '/ mo' : locale === 'ru' ? '/ мес.' : '/ mēn.';
  return `${formatted} ${suffix}`;
}

export function formatPricePerM2(value: number, locale: Locale = 'lv'): string {
  return `${fmtPerM2(locale).format(value)} / m²`;
}

export function formatFloor(
  floor: number,
  total: number | undefined,
  locale: Locale = 'lv'
): string {
  const unit = locale === 'en' ? 'fl' : locale === 'ru' ? 'эт.' : 'st.';
  if (!total) return `${floor} ${unit}`;
  return `${floor}/${total} ${unit}`;
}
