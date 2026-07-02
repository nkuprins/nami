import { computed, ref } from 'vue';
import { getMyListings } from '../../../api/listingsApi';
import type { ListingSummary, ListingType } from '../../../types/listingItem';
import { isExactAddress, isNearAddress } from '../../../utils/addressMatch';

export type DuplicateMatchKind = 'none' | 'exact' | 'fuzzy';

// Duplicate-property guard: stops the user from creating a second physical
// property at a location they already own. An exact address match hard-blocks
// submission (they must view or add a listing to the existing one); a fuzzy
// near-match (likely typo) blocks until the user actively confirms it really is
// a different property.
export function useDuplicatePropertyNudge() {
  const myListings = ref<ListingSummary[] | null>(null);
  const match = ref<ListingSummary | null>(null);
  const matchKind = ref<DuplicateMatchKind>('none');
  const matchTypes = ref<ListingType[]>([]);
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
    matchTypes.value = [];
  }

  async function check(
    address: string,
    district: string | undefined,
    city: string | undefined
  ) {
    if (!address.trim() || !district || !city) {
      reset();
      return;
    }
    await ensureLoaded();
    const candidates = (myListings.value ?? []).filter(
      (item) =>
        item.location.district === district && item.location.city === city
    );
    const exact =
      candidates.find((item) =>
        isExactAddress(item.location.address, address)
      ) ?? null;
    const fuzzy = exact
      ? null
      : (candidates.find((item) =>
          isNearAddress(item.location.address, address)
        ) ?? null);
    const found = exact ?? fuzzy;

    // A fuzzy confirmation only applies to the property it was given for.
    if (found?.propertyId !== match.value?.propertyId)
      acknowledged.value = false;

    match.value = found;
    matchKind.value = exact ? 'exact' : fuzzy ? 'fuzzy' : 'none';
    matchTypes.value = found
      ? (myListings.value ?? [])
          .filter((item) => item.propertyId === found.propertyId)
          .map((item) => item.type)
      : [];
  }

  // The user actively confirmed a fuzzy near-match is a different property.
  function acknowledgeFuzzy() {
    acknowledged.value = true;
  }

  return {
    match,
    matchKind,
    matchTypes,
    acknowledged,
    blockSubmit,
    check,
    acknowledgeFuzzy,
  };
}
