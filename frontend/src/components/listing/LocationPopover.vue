<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { districtSlugByName, slugify } from '../../data/locations';
import {
  City,
  District,
  Location,
  LOCATION_MAP,
} from '../../data/rawLocations';

const { t } = useI18n();

const props = withDefaults(
  defineProps<{
    modelValue: Location[];
    multiple?: boolean;
  }>(),
  {
    multiple: true,
  }
);
const emit = defineEmits<{ 'update:modelValue': [value: Location[]] }>();

const query = ref('');

const grouped = computed(() => {
  const normalizedQuery = slugify(query.value);
  if (!normalizedQuery) return LOCATION_MAP;

  const filteredMap = new Map<City, readonly District[]>();
  for (const [cityName, districts] of LOCATION_MAP) {
    const matchingDistricts = districts.filter((districtName) =>
      districtSlugByName.get(districtName)?.includes(normalizedQuery)
    );

    if (matchingDistricts.length > 0) {
      filteredMap.set(cityName, matchingDistricts);
    }
  }

  return filteredMap;
});

function isSelected(cityName: string, districtName: string): boolean {
  return props.modelValue.some(
    (l) => l.city === cityName && l.district === districtName
  );
}

function handleSelect(cityName: string, districtName: string) {
  let nextValue: Location[];
  const index = props.modelValue.findIndex(
    (l) => l.city === cityName && l.district === districtName
  );

  if (props.multiple) {
    nextValue = [...props.modelValue];

    if (index > -1) {
      nextValue.splice(index, 1);
    } else {
      nextValue.push({ city: cityName, district: districtName });
    }
  } else {
    nextValue = [{ city: cityName, district: districtName }];
  }
  emit('update:modelValue', nextValue);
}

function clear() {
  emit('update:modelValue', []);
  query.value = '';
}
</script>

<template>
  <div class="space-y-4 min-w-70">
    <input
      v-model="query"
      type="search"
      :placeholder="t('filters.findDistrict')"
      class="focus-ring w-full h-10 px-3 rounded-md border border-line bg-bg text-sm placeholder:text-ink-3"
    />
    <div class="space-y-4 max-h-72 overflow-y-auto pr-1">
      <div
        v-for="[cityName, districts] in grouped"
        :key="cityName"
        class="space-y-1.5"
      >
        <p class="micro-label">{{ cityName }}</p>
        <div class="grid grid-cols-2 gap-1">
          <template v-if="multiple">
            <label
              v-for="d in districts"
              :key="d"
              class="focus-ring flex items-center gap-2 px-2 py-1.5 rounded-md text-sm text-ink cursor-pointer hover:bg-surface"
            >
              <input
                type="checkbox"
                class="accent-ink size-4"
                :checked="isSelected(cityName, d)"
                @change="handleSelect(cityName, d)"
              />
              <span>{{ d }}</span>
            </label>
          </template>

          <template v-else>
            <button
              v-for="d in districts"
              :key="d"
              type="button"
              class="focus-ring flex items-center justify-between px-2.5 py-1.5 rounded-md text-sm text-left transition-colors group"
              :class="
                isSelected(cityName, d)
                  ? 'bg-surface text-ink font-semibold'
                  : 'text-ink hover:bg-surface/70'
              "
              @click="handleSelect(cityName, d)"
            >
              <span>{{ d }}</span>
              <span
                v-if="isSelected(cityName, d)"
                class="text-ink text-xs font-bold"
              >
                ✓
              </span>
            </button>
          </template>
        </div>
      </div>
      <p v-if="grouped.size === 0" class="text-sm text-ink-3 py-2">
        {{ t('filters.nothingMatches', { query }) }}
      </p>
    </div>

    <div
      v-if="multiple"
      class="flex items-center justify-between pt-3 border-t border-line"
    >
      <button
        type="button"
        class="focus-ring text-xs text-ink-2 underline underline-offset-4 hover:text-ink"
        @click="clear"
      >
        {{ t('filters.clear') }}
      </button>
      <p class="micro-label">
        {{ modelValue.length || t('filters.any') }} {{ t('filters.selected') }}
      </p>
    </div>
  </div>
</template>
