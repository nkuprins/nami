import type {
  ListingSummary,
  ListingDetail,
  PropertyDetail,
  ListingType,
  PropertyKind,
  PriceInfo,
  PropertyDetails,
  Translations,
  PropertyLocation,
  PropertyMedia,
  Feature,
  PropertyCompletion,
  LocalizedText,
} from '../types/listingItem';
import type { Locale } from '../i18n';
import type { FilterState } from '../types/filter';
import { fetchApi } from './fetchApi';
import {
  toListingSummaryDisplayNames,
  toListingDetailDisplayNames,
  toPropertyDisplayNames,
  toSlugs,
  buildParams,
} from './dto';

export class ListingTypeExistsError extends Error {
  constructor() {
    super('LISTING_TYPE_EXISTS');
  }
}

// Backend rejected a property because the owner already has one at this
// location. `nearDuplicate` distinguishes a fuzzy typo match (overridable with
// confirmedDuplicate) from an exact match (never overridable).
export class DuplicatePropertyError extends Error {
  constructor(public readonly nearDuplicate: boolean) {
    super('DUPLICATE_PROPERTY');
  }
}

export interface CreateListingPayload {
  type: ListingType;
  propertyKind: PropertyKind;
  price: PriceInfo;
  details: PropertyDetails;
  translations: Translations;
  location: PropertyLocation; // display names in, translated to slugs internally
  features: Feature[];
  media: PropertyMedia;
  phones: string[];
  completion?: PropertyCompletion;
  durationMonths: number;
  confirmedDuplicate?: boolean; // set once the user confirms past a fuzzy near-match
}

// A listing is self-contained: adding one at an existing address repeats the
// whole shape (only the location is inherited from the property).
export interface AddListingPayload {
  type: ListingType;
  propertyKind: PropertyKind;
  price: PriceInfo;
  details: PropertyDetails;
  translations: Translations;
  features: Feature[];
  media: PropertyMedia;
  phones: string[];
  completion?: PropertyCompletion;
  durationMonths: number;
}

export interface UpdateListingPayload {
  type: ListingType;
  propertyKind: PropertyKind;
  price: PriceInfo;
  details: PropertyDetails;
  translations: Translations;
  features: Feature[];
  media: PropertyMedia;
  phones: string[];
  completion?: PropertyCompletion;
}

export interface UpdatePropertyPayload {
  location: PropertyLocation; // display names in, translated to slugs internally
}

export async function listListings(
  f: FilterState,
  options?: { signal?: AbortSignal }
): Promise<{ items: ListingSummary[]; total: number }> {
  const res = await fetchApi(`/api/properties?${buildParams(f)}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`listListings: ${res.status}`);
  const data = await res.json();
  return {
    items: data.items.map(toListingSummaryDisplayNames),
    total: data.total,
  };
}

export async function getKindCounts(
  type: ListingType,
  options?: { signal?: AbortSignal }
): Promise<{ apartment: number; house: number }> {
  const res = await fetchApi(`/api/properties/counts?type=${type}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`getKindCounts: ${res.status}`);
  return res.json();
}

export async function getMyListings(): Promise<ListingSummary[]> {
  const res = await fetchApi(`/api/properties/mine`);
  if (!res.ok) throw new Error(`getMyListings: ${res.status}`);
  return (await res.json()).map(toListingSummaryDisplayNames);
}

// With a `locale`, the backend ships only that language's title+description plus
// `availableLocales` (for the language switcher) — used by display surfaces.
// Without one, it ships every locale — used by the edit/copy flow.
export async function getListing(
  id: string,
  locale?: Locale
): Promise<ListingDetail | undefined> {
  const query = locale ? `?locale=${locale}` : '';
  const res = await fetchApi(`/api/properties/${id}${query}`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getListing: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

// One language's title+description, fetched on demand when the detail-page
// language switcher is clicked.
export async function getListingTranslation(
  id: string,
  locale: Locale
): Promise<LocalizedText | undefined> {
  const res = await fetchApi(`/api/properties/${id}/translations/${locale}`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getListingTranslation: ${res.status}`);
  return res.json();
}

// Creates the property + its first listing. `turnstileToken` is the Cloudflare
// human-check token; omitted when Turnstile is not configured (local dev).
export async function createListing(
  data: CreateListingPayload,
  turnstileToken?: string
): Promise<ListingDetail> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (turnstileToken) headers['X-Turnstile-Token'] = turnstileToken;
  const res = await fetchApi(`/api/properties`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...data, location: toSlugs(data.location) }),
  });
  if (res.status === 409) {
    let detail = '';
    try {
      detail = (await res.json())?.detail ?? '';
    } catch {
      // no body — treat as an exact (non-overridable) duplicate
    }
    throw new DuplicatePropertyError(detail.includes('NEAR_DUPLICATE'));
  }
  if (!res.ok) throw new Error(`createListing: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

// Adds a second (or third) listing type to a property the user already owns.
export async function addListing(
  propertyId: string,
  data: AddListingPayload
): Promise<ListingDetail> {
  const res = await fetchApi(`/api/properties/${propertyId}/listings`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (res.status === 409) throw new ListingTypeExistsError();
  if (!res.ok) throw new Error(`addListing: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

export async function renewListing(
  id: string,
  durationMonths: number
): Promise<ListingDetail> {
  const res = await fetchApi(`/api/properties/${id}/renew`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ durationMonths }),
  });
  if (!res.ok) throw new Error(`renewListing: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

export async function updateListing(
  id: string,
  data: UpdateListingPayload
): Promise<ListingDetail> {
  const res = await fetchApi(`/api/properties/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`updateListing: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

export async function getProperty(
  propertyId: string
): Promise<PropertyDetail | undefined> {
  const res = await fetchApi(`/api/properties/${propertyId}/property`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getProperty: ${res.status}`);
  return toPropertyDisplayNames(await res.json());
}

export async function updateProperty(
  propertyId: string,
  data: UpdatePropertyPayload
): Promise<PropertyDetail> {
  const res = await fetchApi(`/api/properties/${propertyId}/property`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...data, location: toSlugs(data.location) }),
  });
  if (!res.ok) throw new Error(`updateProperty: ${res.status}`);
  return toPropertyDisplayNames(await res.json());
}

// Deletes only this listing; the property and any sibling listings survive.
export async function deleteListing(id: string): Promise<void> {
  const res = await fetchApi(`/api/properties/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`deleteListing: ${res.status}`);
}

// Deletes the property and cascades to all of its listings.
export async function deleteProperty(propertyId: string): Promise<void> {
  const res = await fetchApi(`/api/properties/${propertyId}/listings`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error(`deleteProperty: ${res.status}`);
}
