<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue';
import IconSpinner from './IconSpinner.vue';

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

function makeIcon(L: any) {
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

function placeMarker(lat: number, lng: number) {
  const L = (window as any).L;
  if (marker) {
    marker.setLatLng([lat, lng]);
  } else {
    marker = L.marker([lat, lng], {
      icon: makeIcon(L),
      draggable: !props.readonly,
    }).addTo(map);

    if (!props.readonly) {
      marker.on('dragend', () => {
        const ll = marker.getLatLng();
        emit('update:modelValue', { lat: ll.lat, lng: ll.lng });
      });
    }
  }
  map.setView([lat, lng], 16);
}

async function geocode(q: string) {
  if (!q.trim()) return;
  state.value = 'loading';
  try {
    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${encodeURIComponent(q)}`;
    const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
    const data = await res.json();
    if (!data.length) {
      state.value = 'error';
      emit('update:modelValue', null);
      return;
    }
    const lat = parseFloat(data[0].lat);
    const lng = parseFloat(data[0].lon);
    emit('update:modelValue', { lat, lng });
    placeMarker(lat, lng);
    state.value = 'ok';
  } catch {
    state.value = 'error';
    emit('update:modelValue', null);
  }
}

function scheduleGeocode() {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => geocode(query.value), 700);
}

function loadLeaflet(): Promise<void> {
  return new Promise((resolve) => {
    if ((window as any).L) {
      resolve();
      return;
    }
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(link);
    const script = document.createElement('script');
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    script.onload = () => resolve();
    document.head.appendChild(script);
  });
}

onMounted(async () => {
  await loadLeaflet();
  const L = (window as any).L;
  if (!mapEl.value || !L) return;

  map = L.map(mapEl.value, {
    zoomControl: true,
    scrollWheelZoom: false,
  }).setView([56.946, 24.105], 11);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution:
      '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    maxZoom: 19,
  }).addTo(map);

  if (props.modelValue) {
    placeMarker(props.modelValue.lat, props.modelValue.lng);
    state.value = 'ok';
  } else if (query.value.trim()) {
    geocode(query.value);
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

      <Transition name="fade">
        <div
          v-if="state === 'error'"
          class="absolute inset-0 bg-bg/80 backdrop-blur-sm flex items-center justify-center pointer-events-none"
          style="z-index: 1000"
        >
          <p class="text-xs text-ink-3">Location not found on the map</p>
        </div>
      </Transition>
    </div>

    <p v-if="state === 'ok' && !readonly" class="text-xs text-ink-3">
      Pin placed from your address. Drag it to adjust if needed.
    </p>
  </div>
</template>
