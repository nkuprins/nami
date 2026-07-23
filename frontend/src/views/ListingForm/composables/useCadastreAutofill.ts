import { ref, watch, type InjectionKey, type Ref } from 'vue';
import type { LandUse } from '../../../types/listingItem';
import type { ListingFormState } from './formTypes';
import { selectedBuildingCode } from './formHelpers';
import {
  fetchOfficialBuilding,
  fetchOfficialParcel,
} from '../../../api/cadastreApi';

// Official cadastre figures for the currently-selected address, surfaced as
// hints beside the matching form fields. Null where the mirror has no record.
export interface OfficialFigures {
  yearBuilt: number | null;
  area: number | null;
  landM2: number | null;
  landUse: LandUse | null;
}

const EMPTY: OfficialFigures = {
  yearBuilt: null,
  area: null,
  landM2: null,
  landUse: null,
};

// Provided by the listing-form views, injected by the field sections for hints.
export const CADASTRE_OFFICIAL_KEY: InjectionKey<Ref<OfficialFigures>> =
  Symbol('cadastreOfficial');

const DEBOUNCE_MS = 400;

/**
 * Watches the address (register building + apartment) and the cadastral parcel
 * number, fetches the official figures, and:
 *  - prefills the matching form fields when they're empty (or still hold exactly
 *    the value we last auto-filled — so switching address updates them, but a
 *    value the user typed is never overwritten), and
 *  - exposes the official figures for the "official: …" hints.
 */
export function useCadastreAutofill(form: ListingFormState): Ref<OfficialFigures> {
  const official = ref<OfficialFigures>({ ...EMPTY });

  // The last value we auto-filled per field, so we can distinguish our own
  // stale prefill (overwritable) from a value the user typed (preserved).
  const lastFill: Record<'m2' | 'yearBuilt' | 'landM2' | 'landUse', string> = {
    m2: '',
    yearBuilt: '',
    landM2: '',
    landUse: '',
  };

  function prefill(field: 'm2' | 'yearBuilt' | 'landM2' | 'landUse', value: string) {
    if (form[field] === '' || form[field] === lastFill[field]) {
      form[field] = value as never;
      lastFill[field] = value;
    }
  }

  let buildingTimer: ReturnType<typeof setTimeout> | undefined;
  watch(
    () => [selectedBuildingCode(form), form.apartment.trim()] as const,
    ([code, apartment]) => {
      clearTimeout(buildingTimer);
      if (code == null) {
        official.value = { ...official.value, yearBuilt: null, area: null };
        return;
      }
      buildingTimer = setTimeout(async () => {
        try {
          const b = await fetchOfficialBuilding(code, apartment);
          official.value = { ...official.value, yearBuilt: b.yearBuilt, area: b.area };
          if (b.yearBuilt != null) prefill('yearBuilt', String(b.yearBuilt));
          if (b.area != null) prefill('m2', String(b.area));
        } catch {
          // Auto-fill is best-effort; a lookup failure must never block the form.
        }
      }, DEBOUNCE_MS);
    },
    { immediate: true }
  );

  let parcelTimer: ReturnType<typeof setTimeout> | undefined;
  watch(
    () => form.cadastreParcelNr.trim(),
    (parcelNr) => {
      clearTimeout(parcelTimer);
      if (!parcelNr) {
        official.value = { ...official.value, landM2: null, landUse: null };
        return;
      }
      parcelTimer = setTimeout(async () => {
        try {
          const p = await fetchOfficialParcel(parcelNr);
          official.value = { ...official.value, landM2: p.areaM2, landUse: p.landUse };
          if (p.areaM2 != null) prefill('landM2', String(p.areaM2));
          if (p.landUse != null) prefill('landUse', p.landUse);
        } catch {
          // best-effort, see above
        }
      }, DEBOUNCE_MS);
    },
    { immediate: true }
  );

  return official;
}
