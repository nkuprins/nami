import type {
  ListingSummary,
  ListingDetail,
  PropertyDetail,
  PropertyLocation,
} from '../types/listingItem';
import type { FilterState } from '../types/filter';
import {
  cityBySlug,
  cityByName,
  districtNameBySlug,
  districtSlugByName,
} from '../data/locations';

// The API speaks slugs ("agenskalns", "riga") for location.district/location.city.

export type ListingSummaryDto = ListingSummary;
export type ListingDetailDto = ListingDetail;

function withDisplayNames<
  T extends { location: { district: string; city: string } },
>(dto: T): T {
  return {
    ...dto,
    location: {
      ...dto.location,
      district:
        districtNameBySlug.get(dto.location.district) ?? dto.location.district,
      city: cityBySlug.get(dto.location.city) ?? dto.location.city,
    },
  };
}

export function toListingSummaryDisplayNames(
  dto: ListingSummaryDto
): ListingSummary {
  return withDisplayNames(dto);
}

export function toListingDetailDisplayNames(
  dto: ListingDetailDto
): ListingDetail {
  return withDisplayNames(dto);
}

export type PropertyDetailDto = PropertyDetail;

export function toPropertyDisplayNames(dto: PropertyDetailDto): PropertyDetail {
  return withDisplayNames(dto);
}

export function toSlugs(loc: PropertyLocation): PropertyLocation {
  return {
    ...loc,
    district:
      districtSlugByName.get(loc.district) ?? loc.district.toLowerCase(),
    city: cityByName.get(loc.city) ?? loc.city.toLowerCase(),
  };
}

export function buildParams(f: FilterState): URLSearchParams {
  const p = new URLSearchParams();
  p.set('type', f.type);
  f.loc.forEach((l) => p.append('loc', `${l.city}:${l.district}`));
  if (f.priceMin != null) p.set('priceMin', String(f.priceMin));
  if (f.priceMax != null) p.set('priceMax', String(f.priceMax));
  f.rooms.forEach((r) => p.append('rooms', String(r)));
  f.bedrooms.forEach((r) => p.append('bedrooms', String(r)));
  f.bathrooms.forEach((r) => p.append('bathrooms', String(r)));
  if (f.m2Min != null) p.set('m2Min', String(f.m2Min));
  if (f.m2Max != null) p.set('m2Max', String(f.m2Max));
  if (f.floorMin != null) p.set('floorMin', String(f.floorMin));
  if (f.floorMax != null) p.set('floorMax', String(f.floorMax));
  if (f.notGround) p.set('notGround', 'true');
  if (f.notTop) p.set('notTop', 'true');
  if (f.yearMin != null) p.set('yearMin', String(f.yearMin));
  if (f.yearMax != null) p.set('yearMax', String(f.yearMax));
  f.heating.forEach((h) => p.append('heating', h));
  f.energyClass.forEach((e) => p.append('energyClass', e));
  f.features.forEach((ft) => p.append('features', ft));
  if (f.completion) p.set('completion', f.completion);
  p.set('sort', f.sort);
  p.set('page', String(f.page));
  return p;
}
