<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import 'leaflet.markercluster';
import 'leaflet.markercluster/dist/MarkerCluster.css';
import 'leaflet.markercluster/dist/MarkerCluster.Default.css';
import { useI18n } from 'vue-i18n';
import { useMapView } from '../../composables/useMapView';
import ListingRow from './ListingRow.vue';
import ListingCard from './ListingCard.vue';
import IconSpinner from '../icons/IconSpinner.vue';
import type { MapPin } from '../../types/map';

const props = defineProps<{
  pins: MapPin[];
  loading: boolean;
  // Changes when the filter set changes → recenter on the new results.
  fitKey: string;
}>();

const { t } = useI18n();
const { toggleFull, close } = useMapView();

// All-Latvia fallback view when there are no pins to fit.
const LATVIA_CENTER: [number, number] = [56.879, 24.603];
const LATVIA_ZOOM = 7;
const MAX_ZOOM = 19;
const FIT_MAX_ZOOM = 15;
const VIEW_KEY = 'baltnami:mapview';

const mapEl = ref<HTMLDivElement | null>(null);
// The property whose listings are shown in the overlay card (null = none open).
const selected = ref<MapPin | null>(null);

let map: L.Map | null = null;
let cluster: L.MarkerClusterGroup | null = null;
let resizeObserver: ResizeObserver | null = null;
// The fitKey we last centered on, so a pure refetch (same filters) doesn't
// yank the map back while the user is panning.
let centeredKey: string | null = null;

function pinIcon(): L.DivIcon {
  const html = `
    <div style="filter: drop-shadow(0 3px 6px rgba(20,17,13,0.4));">
      <svg xmlns="http://www.w3.org/2000/svg" width="30" height="36" viewBox="0 0 36 42" fill="none">
        <path d="M18 2C10.268 2 4 8.268 4 16c0 10.5 14 28 14 28S32 26.5 32 16C32 8.268 25.732 2 18 2z"
              fill="#f59e0b" stroke="#14110d" stroke-width="2" stroke-linejoin="round"/>
        <circle cx="18" cy="16" r="6" fill="#14110d"/>
      </svg>
    </div>`;
  return L.divIcon({
    className: '',
    html,
    iconSize: [30, 36],
    iconAnchor: [15, 36],
  });
}

function renderPins() {
  if (!map || !cluster) return;
  cluster.clearLayers();
  const markers = props.pins.map((pin) => {
    const marker = L.marker([pin.lat, pin.lng], { icon: pinIcon() });
    marker.on('click', () => {
      selected.value = pin;
    });
    return marker;
  });
  cluster.addLayers(markers);
}

function recenter() {
  if (!map) return;
  if (props.pins.length === 0) {
    map.setView(LATVIA_CENTER, LATVIA_ZOOM);
    return;
  }
  const bounds = L.latLngBounds(props.pins.map((p) => [p.lat, p.lng]));
  map.fitBounds(bounds, { padding: [48, 48], maxZoom: FIT_MAX_ZOOM });
}

function persistView() {
  if (!map) return;
  const c = map.getCenter();
  localStorage.setItem(
    VIEW_KEY,
    JSON.stringify({ lat: c.lat, lng: c.lng, zoom: map.getZoom(), key: props.fitKey })
  );
}

// Restore the last position only when the filter set is unchanged since; a
// changed filter falls through to fitBounds on the fresh results.
function restoreView(): boolean {
  const raw = localStorage.getItem(VIEW_KEY);
  if (!raw || !map) return false;
  try {
    const v = JSON.parse(raw);
    if (v.key === props.fitKey && typeof v.lat === 'number') {
      map.setView([v.lat, v.lng], v.zoom);
      centeredKey = props.fitKey;
      return true;
    }
  } catch {
    /* ignore corrupt view */
  }
  return false;
}

onMounted(() => {
  if (!mapEl.value) return;
  requestAnimationFrame(() => {
    if (!mapEl.value) return;

    map = L.map(mapEl.value, { zoomControl: true, scrollWheelZoom: true }).setView(
      LATVIA_CENTER,
      LATVIA_ZOOM
    );

    L.tileLayer(
      'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',
      {
        attribution:
          '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors © <a href="https://carto.com/">CARTO</a>',
        subdomains: 'abcd',
        maxZoom: MAX_ZOOM,
      }
    ).addTo(map);

    cluster = L.markerClusterGroup({ showCoverageOnHover: false, maxClusterRadius: 50 });
    map.addLayer(cluster);

    resizeObserver = new ResizeObserver(() => map?.invalidateSize());
    resizeObserver.observe(mapEl.value);

    map.on('moveend', persistView);
    // Tapping empty map dismisses the open card.
    map.on('click', () => {
      selected.value = null;
    });

    renderPins();
    if (!restoreView()) recenter();
    if (props.pins.length > 0) centeredKey = props.fitKey;
  });
});

// Rebuild markers whenever the pin set changes.
watch(
  () => props.pins,
  () => {
    selected.value = null; // stale property may no longer be in the results
    renderPins();
    // Recenter only when the filter set (fitKey) changed, not on a plain refetch.
    if (props.fitKey !== centeredKey) {
      recenter();
      centeredKey = props.fitKey;
    }
  }
);

onUnmounted(() => {
  resizeObserver?.disconnect();
  if (map) {
    map.remove();
    map = null;
    cluster = null;
  }
});
</script>

<template>
  <div class="relative w-full h-full">
    <div ref="mapEl" class="w-full h-full" style="isolation: isolate" />

    <!-- Controls: fullscreen toggle + close -->
    <div class="absolute top-3 right-3 flex flex-col gap-2" style="z-index: 1000">
      <button
        type="button"
        @click="toggleFull"
        :aria-label="t('map.fullscreen')"
        class="focus-ring size-10 grid place-items-center rounded-full bg-bg/95 backdrop-blur border border-line-2 text-ink hover:bg-bg shadow-soft"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3M3 16v3a2 2 0 0 0 2 2h3m8 0h3a2 2 0 0 0 2-2v-3" />
        </svg>
      </button>
      <button
        type="button"
        @click="close"
        :aria-label="t('map.close')"
        class="focus-ring size-10 grid place-items-center rounded-full bg-bg/95 backdrop-blur border border-line-2 text-ink hover:bg-bg shadow-soft"
      >
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M18 6 6 18M6 6l12 12" />
        </svg>
      </button>
    </div>

    <Transition name="fade">
      <div
        v-if="loading"
        class="absolute top-3 left-3 flex items-center gap-2 rounded-full bg-bg/95 backdrop-blur px-3 py-1.5 border border-line-2 shadow-soft pointer-events-none"
        style="z-index: 1000"
      >
        <IconSpinner :size="14" class="text-ink-3" />
        <span class="text-xs text-ink-2">{{ t('map.updating') }}</span>
      </div>
    </Transition>

    <Transition name="fade">
      <div
        v-if="!loading && pins.length === 0"
        class="absolute top-3 left-1/2 -translate-x-1/2 rounded-full bg-bg/95 backdrop-blur px-4 py-1.5 border border-line-2 shadow-soft pointer-events-none"
        style="z-index: 1000"
      >
        <span class="text-xs text-ink-2">{{ t('map.noMatches') }}</span>
      </div>
    </Transition>

    <!-- Property card overlay: one listing → a row card; several → a titled shelf of cards. -->
    <Transition name="pop">
      <div
        v-if="selected"
        class="absolute inset-x-0 bottom-0 flex justify-center p-3 sm:p-4 pointer-events-none"
        style="z-index: 1000"
      >
        <div class="pointer-events-auto relative w-full max-w-3xl">
          <button
            type="button"
            @click="selected = null"
            :aria-label="t('map.close')"
            class="focus-ring absolute -top-3 -right-3 z-10 size-9 grid place-items-center rounded-full bg-bg border border-line-2 text-ink shadow-soft hover:bg-surface"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>

          <ListingRow
            v-if="selected.listings.length === 1"
            :property="selected.listings[0]"
          />

          <div
            v-else
            class="bg-bg rounded-2xl shadow-soft border border-line-2 p-4"
          >
            <p class="text-[0.95rem] font-medium text-ink mb-3 truncate pr-8">
              {{ selected.listings[0].location.address }},
              {{ selected.listings[0].location.district }}
              <span class="text-ink-3 font-normal"
                >({{ selected.listings.length }})</span
              >
            </p>
            <div class="flex gap-4 overflow-x-auto pb-1">
              <div
                v-for="lst in selected.listings"
                :key="lst.id"
                class="w-56 sm:w-60 shrink-0"
              >
                <ListingCard :property="lst" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.pop-enter-active,
.pop-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.18s ease;
}
.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>
