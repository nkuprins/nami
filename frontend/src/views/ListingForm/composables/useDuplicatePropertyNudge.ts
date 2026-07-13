import { computed, ref } from 'vue';
import { getMyListings } from '../../../api/listingsApi';
import type { ListingSummary } from '../../../types/listingItem';
import {
  isExactAddress,
  isNearAddress,
  normalizeApartment,
} from '../../../utils/addressMatch';

export type DuplicateMatchKind = 'none' | 'exact' | 'fuzzy';

export interface DuplicateCheckInput {
  // Present when the address was picked from the State Address Register.
  arBuildingCode: number | undefined;
  apartment: string;
  // Composed display address, used against legacy free-text properties.
  address: string;
  district: string | undefined;
  city: string | undefined;
}

// Duplicate-property guard: stops the user from creating a second physical
// property at a location they already own. Register-linked addresses compare
// building + apartment exactly for a hard block; within the same building, a
// near-matching apartment string (likely typo, e.g. "5" vs "5a") blocks until
// confirmed, while a clearly different apartment number is left alone.
// Legacy free-text properties keep the same fuzzy match applied to the whole
// address: an exact normalized match hard-blocks, a near-match blocks until
// the user actively confirms it really is a different property.
export function useDuplicatePropertyNudge() {
  const myListings = ref<ListingSummary[] | null>(null);
  const match = ref<ListingSummary | null>(null);
  const matchKind = ref<DuplicateMatchKind>('none');
  const acknowledged = ref(false);

  const blockSubmit = computed(
    () =>
      matchKind.value === 'exact' ||
      (matchKind.value === 'fuzzy' && !acknowledged.value)
  );

  async function ensureLoaded() {
    if (myListings.value !== null) return;
    try {
      myListings.value = await getMyListings();
    } catch {
      myListings.value = [];
    }
  }

  function reset() {
    match.value = null;
    matchKind.value = 'none';
  }

  async function check(input: DuplicateCheckInput) {
    const { arBuildingCode, address, district, city } = input;
    if (!address.trim() || !district || !city) {
      reset();
      return;
    }
    await ensureLoaded();
    const mine = myListings.value ?? [];

    // Register-linked vs register-linked: same building + same apartment is an
    // exact duplicate; same building with a near-matching apartment string
    // (likely typo) is a fuzzy match; a different building or a clearly
    // different apartment number is legitimately different.
    let exact: ListingSummary | null = null;
    let registerFuzzy: ListingSummary | null = null;
    if (arBuildingCode != null) {
      const apartment = normalizeApartment(input.apartment);
      const sameBuilding = mine.filter(
        (item) => item.location.arBuildingCode === arBuildingCode
      );
      exact =
        sameBuilding.find(
          (item) => normalizeApartment(item.location.apartment) === apartment
        ) ?? null;
      registerFuzzy = exact
        ? null
        : (sameBuilding.find((item) =>
            isNearAddress(
              normalizeApartment(item.location.apartment),
              apartment
            )
          ) ?? null);
    }

    // Legacy free-text properties keep the string match within city+district.
    const legacy = mine.filter(
      (item) =>
        item.location.arBuildingCode == null &&
        item.location.district === district &&
        item.location.city === city
    );
    exact ??=
      legacy.find((item) => isExactAddress(item.location.address, address)) ??
      null;
    const fuzzy = exact
      ? null
      : (registerFuzzy ??
        (legacy.find((item) => isNearAddress(item.location.address, address)) ??
          null));
    const found = exact ?? fuzzy;

    // A fuzzy confirmation only applies to the property it was given for.
    if (found?.propertyId !== match.value?.propertyId)
      acknowledged.value = false;

    match.value = found;
    if (exact) matchKind.value = 'exact';
    else if (fuzzy) matchKind.value = 'fuzzy';
    else matchKind.value = 'none';
  }

  // The user actively confirmed a fuzzy near-match is a different property.
  function acknowledgeFuzzy() {
    acknowledged.value = true;
  }

  return {
    match,
    matchKind,
    acknowledged,
    blockSubmit,
    check,
    acknowledgeFuzzy,
  };
}
