import { computed, ref } from 'vue';
import type { Location } from '../../../data/rawLocations';

export function useLocationDropdown() {
  const selectedLocation = ref<Location | null>(null);
  const isOpen = ref(false);

  const districtName = computed(() =>
    selectedLocation.value
      ? `${selectedLocation.value.district}, ${selectedLocation.value.city}`
      : ''
  );

  function onSelect(locs: Location[]) {
    selectedLocation.value = locs[0] ?? null;
    isOpen.value = false;
  }

  return { selectedLocation, isOpen, districtName, onSelect };
}
