<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';
import { useI18n } from 'vue-i18n';

interface GoogleCredentialResponse {
  credential: string;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize(opts: {
            client_id: string;
            callback: (resp: GoogleCredentialResponse) => void;
          }): void;
          renderButton(el: HTMLElement, opts: Record<string, unknown>): void;
        };
      };
    };
  }
}

const { t } = useI18n();
const emit = defineEmits<{ (e: 'credential', token: string): void }>();

const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID as string | undefined;

// The visible button is ours; Google's real (branding-locked) button is rendered
// transparently on top of it — `overlay` — so it still handles the click and
// returns the ID token, while the user only ever sees the styled button below.
const wrapper = ref<HTMLDivElement | null>(null);
const overlay = ref<HTMLDivElement | null>(null);
let resizeObserver: ResizeObserver | null = null;

// One shared <script> tag across every button mount.
let scriptPromise: Promise<void> | null = null;
function loadGis(): Promise<void> {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = 'https://accounts.google.com/gsi/client';
    s.async = true;
    s.defer = true;
    s.onload = () => resolve();
    s.onerror = () =>
      reject(new Error('Failed to load Google Identity Services'));
    document.head.appendChild(s);
  });
  return scriptPromise;
}

// Google caps its rendered button at 400px; stretch its (invisible) hit area to
// cover our full-width button so a click anywhere on it registers.
function fitOverlay() {
  const w = wrapper.value;
  const o = overlay.value;
  if (!w || !o) return;
  const gis = o.firstElementChild as HTMLElement | null;
  const gw = gis?.offsetWidth || 400;
  const gh = gis?.offsetHeight || 40;
  o.style.transformOrigin = 'top left';
  o.style.transform = `scale(${w.clientWidth / gw}, ${w.clientHeight / gh})`;
}

onMounted(async () => {
  if (!clientId || !overlay.value) return;
  await loadGis();
  if (!overlay.value || !window.google?.accounts?.id) return;
  window.google.accounts.id.initialize({
    client_id: clientId,
    callback: (resp: GoogleCredentialResponse) =>
      emit('credential', resp.credential),
  });
  window.google.accounts.id.renderButton(overlay.value, {
    theme: 'outline',
    size: 'large',
    width: 400,
    text: 'continue_with',
  });
  // GIS renders asynchronously; fit once now and again after it settles.
  requestAnimationFrame(fitOverlay);
  setTimeout(fitOverlay, 200);
  resizeObserver = new ResizeObserver(fitOverlay);
  resizeObserver.observe(wrapper.value!);
});

onBeforeUnmount(() => resizeObserver?.disconnect());
</script>

<template>
  <div
    ref="wrapper"
    class="group relative rounded-lg transition-shadow focus-within:ring-2 focus-within:ring-ink/20"
  >
    <!-- Visible button (decorative; Google's real button overlays it) -->
    <div
      aria-hidden="true"
      class="pointer-events-none flex h-10 w-full items-center justify-center gap-2.5 rounded-lg border border-line-2 bg-bg text-sm font-medium text-ink transition-colors group-hover:bg-surface"
    >
      <svg viewBox="0 0 48 48" class="size-[18px] shrink-0">
        <path
          fill="#EA4335"
          d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"
        />
        <path
          fill="#4285F4"
          d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"
        />
        <path
          fill="#FBBC05"
          d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"
        />
        <path
          fill="#34A853"
          d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"
        />
      </svg>
      <span>{{ t('auth.continueWithGoogle') }}</span>
    </div>
    <div ref="overlay" class="absolute left-0 top-0 opacity-0"></div>
  </div>
</template>
