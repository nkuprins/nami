import type { Category } from './listingItem';

/**
 * Per-category descriptor of which conditional fields/filters apply — the single
 * source of truth the listing form and the search filters both iterate, replacing
 * ad-hoc `v-if` on category. Mirrors the backend registry
 * (`backend/.../category/CategoryProfile.java`) and the DB CHECK constraints;
 * keep the three in sync.
 */
export type FieldMode = 'hidden' | 'optional' | 'required';

export type SubtypeAxis = 'none' | 'newProjectKind' | 'commercial' | 'landUse';

export interface CategoryProfile {
  /** Which second-level select (if any) this category requires. */
  subtype: SubtypeAxis;
  /** rooms + bedrooms/bathrooms. */
  rooms: FieldMode;
  /** m2 — building / floor area. */
  buildingArea: FieldMode;
  /** landM2 — plot / parcel area. */
  plotArea: FieldMode;
  /** floor / totalFloors shown. */
  floors: boolean;
  /** completion (ready/not_ready) — new_project only. */
  completion: boolean;
  /** cadastral parcel selection — land & commercial. */
  parcel: boolean;
}

export const CATEGORY_PROFILES: Record<Category, CategoryProfile> = {
  apartment: {
    subtype: 'none',
    rooms: 'required',
    buildingArea: 'required',
    plotArea: 'hidden',
    floors: true,
    completion: false,
    parcel: false,
  },
  house: {
    subtype: 'none',
    rooms: 'required',
    buildingArea: 'required',
    plotArea: 'optional',
    floors: true,
    completion: false,
    parcel: false,
  },
  new_project: {
    subtype: 'newProjectKind',
    rooms: 'required',
    buildingArea: 'required',
    plotArea: 'hidden',
    floors: true,
    completion: true,
    parcel: false,
  },
  commercial: {
    subtype: 'commercial',
    rooms: 'optional',
    buildingArea: 'required',
    plotArea: 'hidden',
    floors: true,
    completion: false,
    parcel: true,
  },
  land: {
    subtype: 'landUse',
    rooms: 'hidden',
    buildingArea: 'hidden',
    plotArea: 'required',
    floors: false,
    completion: false,
    parcel: true,
  },
  garage: {
    subtype: 'none',
    rooms: 'hidden',
    buildingArea: 'required',
    plotArea: 'hidden',
    floors: false,
    completion: false,
    parcel: false,
  },
};

export function categoryProfile(category: Category): CategoryProfile {
  return CATEGORY_PROFILES[category];
}
