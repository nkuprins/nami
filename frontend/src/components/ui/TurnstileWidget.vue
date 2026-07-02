<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref } from 'vue';

declare global {
  interface Window {
    turnstile?: {
      render(el: HTMLElement, opts: Record<string, unknown>): string;
      reset(id?: string): void;
      remove(id?: string): void;
    };
  }
}

const emit = defineEmits<{
  (e: 'update:modelValue', token: string): void;
}>();

const siteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY as string | undefined;

const container = ref<HTMLDivElement | null>(null);
let widgetId: string | undefined;

// One shared <script> tag across every widget mount.
let scriptPromise: Promise<void> | null = null;
function loadTurnstile(): Promise<void> {
  if (window.turnstile) return Promise.resolve();
  if (scriptPromise) return scriptPromise;
  scriptPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src =
      'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    s.async = true;
    s.defer = true;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Failed to load Turnstile'));
    document.head.appendChild(s);
  });
  return scriptPromise;
}

onMounted(async () => {
  if (!siteKey || !container.value) return;
  await loadTurnstile();
  if (!container.value || !window.turnstile) return;
  widgetId = window.turnstile.render(container.value, {
    sitekey: siteKey,
    callback: (token: string) => emit('update:modelValue', token),
    'expired-callback': () => emit('update:modelValue', ''),
    'error-callback': () => emit('update:modelValue', ''),
  });
});

onBeforeUnmount(() => {
  if (widgetId && window.turnstile) window.turnstile.remove(widgetId);
});

// A Turnstile token is single-use; after a failed submit the widget must be
// reset to issue a fresh one.
function reset() {
  if (widgetId && window.turnstile) {
    window.turnstile.reset(widgetId);
    emit('update:modelValue', '');
  }
}

defineExpose({ reset });
</script>

<template>
  <div ref="container"></div>
</template>
