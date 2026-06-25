import { PropertyType } from '../types/propertyItem';

type Locale = 'lv' | 'en';

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

function fmt(locale: Locale) {
  return locale === 'en' ? fmtEn : fmtLv;
}

export function formatPrice(
  value: number,
  type: PropertyType,
  locale: Locale = 'lv'
): string {
  const formatted = fmt(locale).format(value);
  if (type !== 'rent') return formatted;
  return `${formatted} ${locale === 'en' ? '/ mo' : '/ mēn.'}`;
}

export function formatPricePerM2(value: number, locale: Locale = 'lv'): string {
  return `${fmt(locale).format(value)} / m²`;
}

export function formatFloor(
  floor: number,
  total: number | undefined,
  locale: Locale = 'lv'
): string {
  const unit = locale === 'en' ? 'fl' : 'st.';
  if (!total) return `${floor} ${unit}`;
  return `${floor}/${total} ${unit}`;
}
