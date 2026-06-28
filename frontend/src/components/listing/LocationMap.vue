<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import IconSpinner from '../icons/IconSpinner.vue';

const props = defineProps<{
  address: string;
  district: string;
  city: string;
  modelValue?: { lat: number; lng: number } | null;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: { lat: number; lng: number } | null];
}>();

type GeoState = 'idle' | 'loading' | 'ok' | 'error';

const INITIAL_ZOOM = 11;
const DETAIL_ZOOM = 16;
const MAX_ZOOM = 19;

const mapEl = ref<HTMLDivElement | null>(null);
const state = ref<GeoState>('idle');

let map: any = null;
let marker: any = null;
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

const query = computed(() =>
  [props.address, props.district, props.city, 'Latvia']
    .filter(Boolean)
    .join(', ')
);

// Strip apartment/unit suffixes so "Jēkaba iela 5, apt 3" → "Jēkaba iela 5"
function stripApartment(addr: string): string {
  return addr
    .replace(/,?\s*(apt\.?|apartament[s]?|dz\.?|flat)\s*\d+/i, '')
    .trim();
}

// Build a ranked list of queries to try in order
function buildQueries(): string[] {
  const street = props.address.trim();
  const city = props.city.trim();
  const district = props.district.trim();
  const stripped = stripApartment(street);
  const queries: string[] = [];

  // 1. Full query as-is
  if (street)
    queries.push([street, district, city, 'Latvia'].filter(Boolean).join(', '));
  // 2. Stripped apartment number
  if (stripped && stripped !== street)
    queries.push(
      [stripped, district, city, 'Latvia'].filter(Boolean).join(', ')
    );
  // 3. Street + city only (skip district — sometimes it confuses Nominatim)
  if (stripped)
    queries.push([stripped, city, 'Latvia'].filter(Boolean).join(', '));
  // 4. Just city + country (last resort — at least shows the right city)
  if (city) queries.push([city, 'Latvia'].join(', '));

  return [...new Set(queries)]; // deduplicate
}

function makeIcon() {
  const cursor = props.readonly ? 'default' : 'grab';
  const html = `
    <div style="cursor:${cursor}; filter: drop-shadow(0 3px 6px rgba(20,17,13,0.4));">
      <svg xmlns="http://www.w3.org/2000/svg" width="36" height="42" viewBox="0 0 36 42" fill="none">
        <path d="M18 2C10.268 2 4 8.268 4 16c0 10.5 14 28 14 28S32 26.5 32 16C32 8.268 25.732 2 18 2z"
              fill="#f59e0b" stroke="#14110d" stroke-width="2" stroke-linejoin="round"/>
        <g transform="translate(9, 7)">
          <path d="M9 2L1 8h2v7h12V8h2L9 2z" fill="#14110d" opacity="0.85"/>
          <rect x="6.5" y="11" width="5" height="4" rx="0.5" fill="#f59e0b"/>
        </g>
      </svg>
    </div>`;
  return L.divIcon({
    className: '',
    html,
    iconSize: [36, 42],
    iconAnchor: [18, 42],
  });
}

function placeMarker(lat: number, lng: number, fly = true) {
  if (marker) {
    marker.setLatLng([lat, lng]);
  } else {
    marker = L.marker([lat, lng], {
      icon: makeIcon(),
      draggable: !props.readonly,
    }).addTo(map);

    if (!props.readonly) {
      marker.on('dragend', () => {
        const ll = marker.getLatLng();
        emit('update:modelValue', { lat: ll.lat, lng: ll.lng });
      });
    }
  }
  if (fly) map.setView([lat, lng], DETAIL_ZOOM);
}

// Enable click-to-place when not readonly
function setupClickToPlace() {
  if (props.readonly || !map) return;
  map.on('click', (e: any) => {
    const { lat, lng } = e.latlng;
    placeMarker(lat, lng, false);
    emit('update:modelValue', { lat, lng });
    state.value = 'ok';
  });
}

async function tryGeocode(
  q: string
): Promise<{ lat: number; lng: number } | null> {
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(q)}&countrycodes=lv`;
  const res = await fetch(url, { headers: { 'Accept-Language': 'lv,en' } });
  const data = await res.json();
  if (!data.length) return null;
  return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) };
}

async function geocode() {
  const queries = buildQueries();
  if (!queries.length) return;
  state.value = 'loading';

  for (const q of queries) {
    try {
      const coords = await tryGeocode(q);
      if (coords) {
        emit('update:modelValue', coords);
        placeMarker(coords.lat, coords.lng);
        state.value = 'ok';
        return;
      }
    } catch {
      // try next query
    }
  }

  // All queries failed — show soft error but keep map interactive
  state.value = 'error';
  // Don't emit null if we already had coords — user may have placed manually
  if (!props.modelValue) emit('update:modelValue', null);
}

function scheduleGeocode() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(geocode, 800);
}

onMounted(() => {
  if (!mapEl.value) return;

  map = L.map(mapEl.value, {
    zoomControl: true,
    scrollWheelZoom: false,
  }).setView([56.946, 24.105], INITIAL_ZOOM);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution:
      '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: MAX_ZOOM,
  }).addTo(map);

  setupClickToPlace();

  if (props.modelValue) {
    placeMarker(props.modelValue.lat, props.modelValue.lng);
    state.value = 'ok';
  } else if (query.value.trim()) {
    geocode();
  }
});

onUnmounted(() => {
  if (debounceTimer) clearTimeout(debounceTimer);
  if (map) {
    map.remove();
    map = null;
    marker = null;
  }
});

watch(query, () => {
  if (!props.readonly) scheduleGeocode();
});
</script>

<template>
  <div class="flex flex-col gap-2">
    <div
      class="relative rounded-xl overflow-hidden border border-line w-full"
      style="aspect-ratio: 1 / 1; max-height: 520px; isolation: isolate"
    >
      <div ref="mapEl" class="w-full h-full" />

      <Transition name="fade">
        <div
          v-if="state === 'loading'"
          class="absolute inset-0 bg-bg/70 backdrop-blur-sm flex items-center justify-center pointer-events-none"
          style="z-index: 1000"
        >
          <IconSpinner :size="18" class="text-ink-3" />
        </div>
      </Transition>

      <!-- Error: non-blocking, map stays interactive -->
      <Transition name="fade">
        <div
          v-if="state === 'error'"
          class="absolute bottom-10 left-1/2 -translate-x-1/2 pointer-events-none"
          style="z-index: 1000"
        >
          <p
            class="text-xs text-ink bg-bg/90 backdrop-blur px-3 py-1.5 rounded-full border border-line whitespace-nowrap shadow-soft"
          >
            Address not found — click the map to place pin manually
          </p>
        </div>
      </Transition>
    </div>

    <p v-if="state === 'ok' && !readonly" class="text-xs text-ink-3">
      Pin placed from your address. Drag it to adjust if needed.
    </p>
    <p v-if="state === 'error' && !readonly" class="text-xs text-ink-3">
      Click anywhere on the map to place the pin manually.
    </p>
  </div>
</template>
