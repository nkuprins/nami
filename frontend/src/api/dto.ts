import type { PropertySummary, PropertyDetail } from '../types/propertyItem';
import type { FilterState } from '../types/filter';
import {
  cityBySlug,
  cityByName,
  districtNameBySlug,
  districtSlugByName,
} from '../data/locations';

// The API speaks slugs ("agenskalns", "riga").

export type PropertySummaryDto = Omit<PropertySummary, 'district' | 'city'> & {
  district: string; // slug
  city: string; // slug
};

export type PropertyDetailDto = Omit<PropertyDetail, 'district' | 'city'> & {
  district: string; // slug
  city: string; // slug
};

export type PropertyPayload = Omit<PropertyDetailDto, 'id' | 'postedAt'>;

export function toSummaryDisplayNames(
  dto: PropertySummaryDto
): PropertySummary {
  return {
    ...dto,
    district: districtNameBySlug.get(dto.district) ?? dto.district,
    city: cityBySlug.get(dto.city) ?? dto.city,
  };
}

export function toDetailDisplayNames(dto: PropertyDetailDto): PropertyDetail {
  return {
    ...dto,
    district: districtNameBySlug.get(dto.district) ?? dto.district,
    city: cityBySlug.get(dto.city) ?? dto.city,
  };
}

export function toSlugs(
  data: Omit<PropertyDetail, 'id' | 'postedAt'>
): PropertyPayload {
  return {
    ...data,
    district:
      districtSlugByName.get(data.district) ?? data.district.toLowerCase(),
    city: cityByName.get(data.city) ?? data.city.toLowerCase(),
  };
}

export function buildParams(f: FilterState): URLSearchParams {
  const p = new URLSearchParams();
  p.set('type', f.type);
  f.loc.forEach((l) => p.append('loc', `${l.city}:${l.district}`));
  if (f.priceMin != null) p.set('priceMin', String(f.priceMin));
  if (f.priceMax != null) p.set('priceMax', String(f.priceMax));
  f.rooms.forEach((r) => p.append('rooms', String(r)));
  if (f.m2Min != null) p.set('m2Min', String(f.m2Min));
  if (f.m2Max != null) p.set('m2Max', String(f.m2Max));
  if (f.floorMin != null) p.set('floorMin', String(f.floorMin));
  if (f.floorMax != null) p.set('floorMax', String(f.floorMax));
  if (f.notGround) p.set('notGround', 'true');
  if (f.notTop) p.set('notTop', 'true');
  if (f.yearMin != null) p.set('yearMin', String(f.yearMin));
  if (f.yearMax != null) p.set('yearMax', String(f.yearMax));
  f.features.forEach((ft) => p.append('features', ft));
  if (f.completion) p.set('completion', f.completion);
  p.set('sort', f.sort);
  p.set('page', String(f.page));
  return p;
}
