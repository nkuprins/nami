import { ref } from 'vue';
import { getMyListings } from '../../../api/listingsApi';
import type { ListingSummary, ListingType } from '../../../types/listingItem';

// Conservative, exact-match duplicate-property detector: nudges the user
// toward adding a listing to a property they already own instead of
// accidentally creating a second physical property for the same address.
export function useDuplicatePropertyNudge() {
  const myListings = ref<ListingSummary[] | null>(null);
  const dismissed = ref(false);
  const match = ref<ListingSummary | null>(null);
  const matchTypes = ref<ListingType[]>([]);

  async function ensureLoaded() {
    if (myListings.value !== null) return;
    try {
      myListings.value = await getMyListings();
    } catch {
      myListings.value = [];
    }
  }

  function normalize(s: string): string {
    return s.trim().toLowerCase().replace(/\s+/g, ' ');
  }

  async function check(
    address: string,
    district: string | undefined,
    city: string | undefined
  ) {
    if (dismissed.value || !address.trim() || !district || !city) {
      match.value = null;
      matchTypes.value = [];
      return;
    }
    await ensureLoaded();
    const target = normalize(address);
    const found =
      (myListings.value ?? []).find(
        (item) =>
          normalize(item.location.address) === target &&
          item.location.district === district &&
          item.location.city === city
      ) ?? null;
    match.value = found;
    matchTypes.value = found
      ? (myListings.value ?? [])
          .filter((item) => item.propertyId === found.propertyId)
          .map((item) => item.type)
      : [];
  }

  function dismiss() {
    dismissed.value = true;
    match.value = null;
    matchTypes.value = [];
  }

  return { match, matchTypes, check, dismiss };
}
