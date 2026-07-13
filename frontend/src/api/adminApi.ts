import type { ListingDetail, ListingSummary } from '../types/listingItem';
import type { Locale } from '../i18n';
import { fetchApi } from './fetchApi';
import {
  toListingDetailDisplayNames,
  toListingSummaryDisplayNames,
} from './dto';

export async function getPendingListings(): Promise<ListingSummary[]> {
  const res = await fetchApi(`/api/admin/listings/pending`);
  if (!res.ok) throw new Error(`getPendingListings: ${res.status}`);
  return (await res.json()).map(toListingSummaryDisplayNames);
}

export async function approveListing(id: string): Promise<void> {
  const res = await fetchApi(`/api/admin/listings/${id}/approve`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`approveListing: ${res.status}`);
}

export async function rejectListing(id: string): Promise<void> {
  const res = await fetchApi(`/api/admin/listings/${id}/reject`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`rejectListing: ${res.status}`);
}

// Bypasses the ACTIVE-only filter on the public listing endpoint, so an admin can
// still view (and reactivate) a suspended listing.
export async function getListingForAdmin(
  id: string,
  locale?: Locale
): Promise<ListingDetail | undefined> {
  const query = locale ? `?locale=${locale}` : '';
  const res = await fetchApi(`/api/admin/listings/${id}${query}`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getListingForAdmin: ${res.status}`);
  return toListingDetailDisplayNames(await res.json());
}

export async function suspendListing(id: string): Promise<void> {
  const res = await fetchApi(`/api/admin/listings/${id}/suspend`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`suspendListing: ${res.status}`);
}

export async function reactivateListing(id: string): Promise<void> {
  const res = await fetchApi(`/api/admin/listings/${id}/reactivate`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`reactivateListing: ${res.status}`);
}
