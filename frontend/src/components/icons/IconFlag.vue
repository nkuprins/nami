<script setup lang="ts">
import type { Locale } from '../../i18n';

defineProps<{ locale: Locale }>();
</script>

<template>
  <svg
    viewBox="0 0 20 14"
    aria-hidden="true"
    class="size-full rounded-[2.5px] overflow-hidden ring-1 ring-inset ring-ink/10"
  >
    <template v-if="locale === 'lv'">
      <rect width="20" height="14" fill="#fff" />
      <rect width="20" height="6" fill="#9e3039" />
      <rect y="8" width="20" height="6" fill="#9e3039" />
    </template>

    <template v-else-if="locale === 'ru'">
      <rect width="20" height="14" fill="#fff" />
      <rect y="4.67" width="20" height="4.67" fill="#0039a6" />
      <rect y="9.33" width="20" height="4.67" fill="#d52b1e" />
    </template>

    <template v-else>
      <rect width="20" height="14" fill="#00247d" />
      <g stroke="#fff" stroke-width="2.8">
        <path d="M0 0 20 14M20 0 0 14" />
      </g>
      <g stroke="#cf142b" stroke-width="1.4">
        <path d="M0 0 20 14M20 0 0 14" />
      </g>
      <g stroke="#fff" stroke-width="4.6">
        <path d="M10 0V14M0 7H20" />
      </g>
      <g stroke="#cf142b" stroke-width="2.6">
        <path d="M10 0V14M0 7H20" />
      </g>
    </template>
  </svg>
</template>
