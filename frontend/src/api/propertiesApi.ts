import type { PropertySummary, PropertyDetail } from '../types/propertyItem';
import type { FilterState } from '../types/filter';
import { fetchApi } from './fetchApi';
import {
  toSummaryDisplayNames,
  toDetailDisplayNames,
  toSlugs,
  buildParams,
} from './dto';

export async function listProperties(
  f: FilterState,
  options?: { signal?: AbortSignal }
): Promise<{ items: PropertySummary[]; total: number }> {
  const res = await fetchApi(`/api/properties?${buildParams(f)}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`listProperties: ${res.status}`);
  const data = await res.json();
  return { items: data.items.map(toSummaryDisplayNames), total: data.total };
}

export async function getMyProperties(): Promise<PropertySummary[]> {
  const res = await fetchApi(`/api/properties/mine`);
  if (!res.ok) throw new Error(`getMyProperties: ${res.status}`);
  return (await res.json()).map(toSummaryDisplayNames);
}

export async function getProperty(
  id: string
): Promise<PropertyDetail | undefined> {
  const res = await fetchApi(`/api/properties/${id}`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getProperty: ${res.status}`);
  return toDetailDisplayNames(await res.json());
}

export async function addProperty(
  data: Omit<PropertyDetail, 'id' | 'postedAt'>
): Promise<PropertyDetail> {
  const res = await fetchApi(`/api/properties`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toSlugs(data)),
  });
  if (!res.ok) throw new Error(`addProperty: ${res.status}`);
  return toDetailDisplayNames(await res.json());
}

export async function updateProperty(
  id: string,
  data: Omit<
    PropertyDetail,
    'id' | 'postedAt' | 'district' | 'city' | 'address' | 'coords' | 'photos'
  >
): Promise<PropertyDetail> {
  const res = await fetchApi(`/api/properties/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error(`updateProperty: ${res.status}`);
  return toDetailDisplayNames(await res.json());
}

export async function deleteProperty(id: string): Promise<void> {
  const res = await fetchApi(`/api/properties/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`deleteProperty: ${res.status}`);
}
