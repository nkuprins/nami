import type { ListingSummary } from '../types/listingItem';
import { fetchApi } from './fetchApi';
import { toListingSummaryDisplayNames } from './dto';

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
