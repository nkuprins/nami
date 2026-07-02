import type { Locale } from '../i18n';
import { ListingType } from '../types/listingItem';

const fmtLv = new Intl.NumberFormat('lv-LV', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

const fmtEn = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

const fmtRu = new Intl.NumberFormat('ru-RU', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

function fmt(locale: Locale) {
  if (locale === 'en') return fmtEn;
  if (locale === 'ru') return fmtRu;
  return fmtLv;
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
  return `${fmt(locale).format(value)} / m²`;
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
