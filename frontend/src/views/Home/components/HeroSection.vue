<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import SearchPanel from './SearchPanel.vue';

const { t } = useI18n();
const emit = defineEmits<{ search: []; openMore: [] }>();
</script>

<template>
  <section class="relative isolate overflow-hidden bg-ink">
    <div class="absolute inset-0 -z-10">
      <img
        src="https://picsum.photos/seed/baltnami-hero-rooftop/2400/1400"
        alt=""
        class="size-full object-cover"
      />
      <div
        class="absolute inset-0 bg-linear-to-b from-ink/55 via-ink/40 to-ink/85"
      />
      <div
        class="absolute inset-0 bg-linear-to-r from-ink/55 via-ink/10 to-transparent"
      />
    </div>
    <div class="absolute inset-0 -z-10 grain pointer-events-none" />

    <div class="mx-auto max-w-360 px-6 lg:px-10 pt-20 pb-44 sm:pb-48 lg:pt-32">
      <div
        class="flex items-center gap-6 micro-label font-bold text-cream/70! mb-10"
      >
        <span>{{ t('hero.country') }}</span>
        <span class="hidden md:inline h-px w-12 bg-cream/30" />
        <span class="hidden md:inline">{{ t('hero.curated') }}</span>
      </div>

      <h1
        class="display-headline text-cream text-[clamp(2.75rem,6.4vw,5.75rem)] max-w-4xl"
      >
        {{ t('hero.headline') }}
        <em class="display-eyebrow text-accent">{{ t('hero.accent') }}</em>
      </h1>

      <p
        class="mt-3 max-w-xl text-cream/80 text-base sm:text-lg leading-relaxed"
      >
        {{ t('hero.subtitle') }}
      </p>

      <div
        class="mt-8 hidden sm:flex items-center gap-8 text-cream/60 text-xs font-bold"
      >
        <div class="flex items-baseline gap-2">
          <span class="display-price text-cream text-2xl">10</span>
          <span class="micro-label text-cream/60!">{{
            t('hero.properties')
          }}</span>
        </div>
        <div class="h-8 w-px bg-cream/20" />
        <div class="flex items-baseline gap-2">
          <span class="display-price text-cream text-2xl">100</span>
          <span class="micro-label text-cream/60!">{{
            t('hero.districts')
          }}</span>
        </div>
        <div class="h-8 w-px bg-cream/20" />
        <div class="flex items-baseline gap-2">
          <span class="display-price text-cream text-2xl">12</span>
          <span class="micro-label text-cream/60!">{{
            t('hero.clients')
          }}</span>
        </div>
      </div>
    </div>

    <div class="relative mx-auto max-w-7xl px-6 lg:px-10 -mt-28 sm:-mt-32 pb-6">
      <SearchPanel @search="emit('search')" @open-more="emit('openMore')" />
    </div>
  </section>
</template>
