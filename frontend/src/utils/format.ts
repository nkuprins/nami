import { PropertyType } from '../types/propertyItem';

const euroFmt = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
});

export function formatPrice(value: number, type: PropertyType): string {
  return type === 'rent'
    ? `${euroFmt.format(value)} / mo`
    : euroFmt.format(value);
}

export function formatPricePerM2(value: number): string {
  return `${euroFmt.format(value)} / m²`;
}
export function formatFloor(floor: number, total: number | undefined): string {
  if (!total) return `${floor} fl`;
  return `${floor}/${total} fl`;
}
