import type { LandUse } from '../types/listingItem';
import { fetchApi } from './fetchApi';

// Official VZD-cadastre figures for a selected address; fields are null when the
// mirror has no record. Used to auto-fill and annotate the listing form.
export interface OfficialBuilding {
  yearBuilt: number | null;
  area: number | null;
}

export interface OfficialParcel {
  areaM2: number | null;
  landUse: LandUse | null;
}

export async function fetchOfficialBuilding(
  buildingCode: number,
  apartment: string
): Promise<OfficialBuilding> {
  const q = new URLSearchParams({ buildingCode: String(buildingCode) });
  if (apartment) q.set('apartment', apartment);
  const res = await fetchApi(`/api/cadastre/building?${q}`);
  if (!res.ok) throw new Error(`fetchOfficialBuilding: ${res.status}`);
  return res.json();
}

export async function fetchOfficialParcel(
  parcelNr: string
): Promise<OfficialParcel> {
  const res = await fetchApi(
    `/api/cadastre/parcel?parcelNr=${encodeURIComponent(parcelNr)}`
  );
  if (!res.ok) throw new Error(`fetchOfficialParcel: ${res.status}`);
  return res.json();
}
