import {mockListings} from '../mock/listings';
import {PropertyItem} from "../types/propertyItem";
import {FilterState, PAGE_SIZE} from "../types/filter";

function inRange(value: number, min?: number, max?: number): boolean {
    if (min !== undefined && value < min) return false;
    return !(max !== undefined && value > max);
}


function matches(p: PropertyItem, f: FilterState): boolean {
    if (p.type !== f.type) return false;
    if (!f.loc.includes(p.district)) return false;

    if (!inRange(p.price, f.priceMin, f.priceMax)) return false;
    if (!inRange(p.m2, f.m2Min, f.m2Max)) return false;
    if (!inRange(p.floor ?? 0, f.floorMin, f.floorMax)) return false;
    if (!inRange(p.yearBuilt ?? 0, f.yearMin, f.yearMax)) return false;

    if (f.rooms !== undefined && p.rooms !== f.rooms) return false;
    if (f.notGround && p.floor === 1) return false;
    if (f.notTop && p.floor === p.totalFloors && p.floor != null) return false;
    if (f.features.length && !f.features.every((feat) => p.features.includes(feat))) return false;

    return !(f.type === 'new-project' && f.completion && p.completion !== f.completion);
}

function sortListings(items: PropertyItem[], sort: FilterState['sort']): PropertyItem[] {
    const out = [...items];
    switch (sort) {
        case 'price-asc':
            out.sort((a, b) => a.price - b.price);
            break;
        case 'price-desc':
            out.sort((a, b) => b.price - a.price);
            break;
        case 'price-per-m2-asc':
            out.sort((a, b) => (a.price / a.m2) - (b.price / b.m2));
            break;
        case 'm2-desc':
            out.sort((a, b) => b.m2 - a.m2);
            break;
        case 'newest':
        default:
            out.sort((a, b) => new Date(b.postedAt).getTime() - new Date(a.postedAt).getTime());
    }
    return out;
}

const ARTIFICIAL_DELAY_MS = 120;

function delay<T>(value: T): Promise<T> {
    return new Promise((resolve) => setTimeout(() => resolve(value), ARTIFICIAL_DELAY_MS));
}

export async function listProperties(f: FilterState): Promise<{ items: PropertyItem[]; total: number }> {
    const filtered = mockListings.filter((p) => matches(p, f));
    const sorted = sortListings(filtered, f.sort);
    const start = (f.page - 1) * PAGE_SIZE;
    const items = sorted.slice(start, start + PAGE_SIZE);
    return delay({items, total: sorted.length});
}