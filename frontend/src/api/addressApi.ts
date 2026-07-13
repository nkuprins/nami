import { fetchApi } from './fetchApi';
import { cityByName, districtSlugByName } from '../data/locations';

// Typeahead over the backend's State Address Register mirror. Streets and
// house numbers are picked, never typed free-form — the selected codes anchor
// the property to an official register building.

export interface StreetOption {
  // 'street': a register street — pick a house number next.
  // 'house': a rural house named directly under its territory — terminal pick.
  kind: 'street' | 'house';
  code: number;
  name: string;
  territory: string;
  lat?: number;
  lng?: number;
}

export interface BuildingOption {
  code: number;
  name: string;
  lat?: number;
  lng?: number;
}

export async function searchStreets(
  city: string,
  district: string,
  q: string,
  options?: { signal?: AbortSignal }
): Promise<StreetOption[]> {
  const params = new URLSearchParams({
    city: cityByName.get(city) ?? city.toLowerCase(),
    district: districtSlugByName.get(district) ?? district.toLowerCase(),
    q,
  });
  const res = await fetchApi(`/api/address/streets?${params}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`searchStreets: ${res.status}`);
  return res.json();
}

export async function searchBuildings(
  streetCode: number,
  q: string,
  options?: { signal?: AbortSignal }
): Promise<BuildingOption[]> {
  const params = new URLSearchParams({ streetCode: String(streetCode), q });
  const res = await fetchApi(`/api/address/buildings?${params}`, {
    signal: options?.signal,
  });
  if (!res.ok) throw new Error(`searchBuildings: ${res.status}`);
  return res.json();
}
