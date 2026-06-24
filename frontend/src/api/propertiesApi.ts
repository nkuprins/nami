import type { PropertyItem } from '../types/propertyItem';
import type { FilterState } from '../types/filter';
import { fetchApi } from './fetchApi';
import { toDisplayNames, toSlugs, buildParams } from './dto';

export async function listProperties(
  f: FilterState,
  options?: { signal?: AbortSignal }
): Promise<{ items: PropertyItem[]; total: number }> {
  const res = await fetchApi(`/api/properties?${buildParams(f)}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`listProperties: ${res.status}`);
  const data = await res.json();
  return { items: data.items.map(toDisplayNames), total: data.total };
}

export async function getMyProperties(): Promise<PropertyItem[]> {
  const res = await fetchApi(`/api/properties/mine`);
  if (!res.ok) throw new Error(`getMyProperties: ${res.status}`);
  return (await res.json()).map(toDisplayNames);
}

export async function getProperty(
  id: string
): Promise<PropertyItem | undefined> {
  const res = await fetchApi(`/api/properties/${id}`);
  if (res.status === 404) return undefined;
  if (!res.ok) throw new Error(`getProperty: ${res.status}`);
  return toDisplayNames(await res.json());
}

export async function addProperty(
  data: Omit<PropertyItem, 'id' | 'postedAt'>
): Promise<PropertyItem> {
  const res = await fetchApi(`/api/properties`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(toSlugs(data)),
  });
  if (!res.ok) throw new Error(`addProperty: ${res.status}`);
  return toDisplayNames(await res.json());
}

export async function deleteProperty(id: string): Promise<void> {
  const res = await fetchApi(`/api/properties/${id}`, { method: 'DELETE' });
  if (!res.ok) throw new Error(`deleteProperty: ${res.status}`);
}
