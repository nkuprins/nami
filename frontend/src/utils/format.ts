import {PropertyType} from "../types/propertyItem";

export function isNew(postedAt: string): boolean {
    const posted = new Date(postedAt).getTime();
    const sevenDays = 7 * 24 * 60 * 60 * 1000;
    return Date.now() - posted < sevenDays;
}

const euroFmt = new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
});

const numberFmt = new Intl.NumberFormat('en-IE', {
    maximumFractionDigits: 0,
});

export function formatPrice(value: number, type: PropertyType): string {
    return type === 'rent' ? `${euroFmt.format(value)} / mo` : euroFmt.format(value);
}

export function formatPricePerM2(value: number): string {
    return `${euroFmt.format(value)} / m²`;
}

export function formatM2(value: number): string {
    return `${numberFmt.format(value)} m²`;
}

export function formatFloor(floor: number, total: number | undefined): string {
    if (!total) return `${floor} fl`;
    return `${floor}/${total} fl`;
}

export function relativeTime(iso: string): string {
    const t = new Date(iso).getTime();
    const diff = Date.now() - t;
    const day = 24 * 60 * 60 * 1000;
    if (diff < day) return 'today';
    const days = Math.floor(diff / day);
    if (days < 7) return `${days}d ago`;
    if (days < 30) return `${Math.floor(days / 7)}w ago`;
    return `${Math.floor(days / 30)}mo ago`;
}
