<script setup lang="ts">
import { mediaVariant, onVariantError } from '../../../utils/mediaVariant';

defineProps<{
  photos: string[];
  alt: string;
}>();

const emit = defineEmits<{
  'open-lightbox': [index: number];
}>();
</script>

<template>
  <!-- 3+ photos -->
  <div
    v-if="photos.length >= 3"
    class="hidden lg:grid grid-cols-[2fr_1fr] grid-rows-2 gap-1.5 rounded-xl overflow-hidden h-[420px] mb-8"
  >
    <div
      class="row-span-2 cursor-zoom-in overflow-hidden group"
      @click="emit('open-lightbox', 0)"
    >
      <img
        :src="mediaVariant(photos[0], 'card')"
        :alt="`${alt} — 1`"
        class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        @error="(e) => onVariantError(e, photos[0])"
      />
    </div>
    <div
      class="cursor-zoom-in overflow-hidden group"
      @click="emit('open-lightbox', 1)"
    >
      <img
        :src="mediaVariant(photos[1], 'card')"
        :alt="`${alt} — 2`"
        class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        @error="(e) => onVariantError(e, photos[1])"
      />
    </div>
    <div
      class="relative cursor-zoom-in overflow-hidden group"
      @click="emit('open-lightbox', 2)"
    >
      <img
        :src="mediaVariant(photos[2], 'card')"
        :alt="`${alt} — 3`"
        class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        @error="(e) => onVariantError(e, photos[2])"
      />
      <button
        v-if="photos.length > 3"
        type="button"
        class="absolute bottom-3 right-3 z-10 text-xs font-medium text-ink bg-bg/90 backdrop-blur px-3 py-1.5 rounded-lg border border-line cursor-pointer hover:bg-bg transition-colors"
        @click.stop="emit('open-lightbox', 0)"
      >
        View all {{ photos.length }} photos
      </button>
    </div>
  </div>

  <!-- Exactly 2 photos -->
  <div
    v-else-if="photos.length === 2"
    class="hidden lg:grid grid-cols-2 gap-1.5 rounded-xl overflow-hidden h-[420px] mb-8"
  >
    <div
      class="cursor-zoom-in overflow-hidden group"
      @click="emit('open-lightbox', 0)"
    >
      <img
        :src="mediaVariant(photos[0], 'card')"
        :alt="`${alt} — 1`"
        class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        @error="(e) => onVariantError(e, photos[0])"
      />
    </div>
    <div
      class="cursor-zoom-in overflow-hidden group"
      @click="emit('open-lightbox', 1)"
    >
      <img
        :src="mediaVariant(photos[1], 'card')"
        :alt="`${alt} — 2`"
        class="size-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
        @error="(e) => onVariantError(e, photos[1])"
      />
    </div>
  </div>

  <!-- Exactly 1 photo -->
  <div
    v-else-if="photos.length === 1"
    class="hidden lg:block rounded-xl overflow-hidden mb-8 cursor-zoom-in group"
    @click="emit('open-lightbox', 0)"
  >
    <img
      :src="mediaVariant(photos[0], 'card')"
      :alt="`${alt} — 1`"
      class="w-full max-h-[520px] object-cover transition-transform duration-300 group-hover:scale-[1.02]"
      @error="(e) => onVariantError(e, photos[0])"
    />
  </div>

  <!-- 0 photos -->
  <div
    v-else
    class="hidden lg:flex items-center justify-center rounded-xl overflow-hidden h-[420px] mb-8 bg-surface border border-dashed border-line"
  >
    <p class="text-sm text-ink-3">No photos available</p>
  </div>
</template>
