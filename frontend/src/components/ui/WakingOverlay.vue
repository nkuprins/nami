<script setup lang="ts">
import { isWaking } from '../../composables/useWakeStatus';
import catSvg from '../../assets/sleeping-cat.svg?raw';
</script>

<template>
  <Transition name="wake-fade">
    <output
      v-if="isWaking"
      class="fixed inset-0 z-[100] flex items-center justify-center bg-cream/95 backdrop-blur-sm"
      aria-live="polite"
    >
      <div class="flex max-w-sm flex-col items-center px-6 text-center">
        <!-- eslint-disable-next-line vue/no-v-html -- trusted, bundled asset -->
        <div class="wake-art" v-html="catSvg" aria-hidden="true"></div>

        <h2 class="mt-4 font-display text-xl text-ink">
          {{ $t('waking.title') }}
        </h2>
        <p class="mt-2 text-sm text-ink-2">
          {{ $t('waking.body') }}
        </p>
      </div>
    </output>
  </Transition>
</template>

<style scoped>
.wake-art {
  width: 15rem;
}
.wake-art :deep(svg) {
  display: block;
  width: 100%;
  height: auto;
}

.wake-fade-enter-active,
.wake-fade-leave-active {
  transition: opacity 0.4s ease;
}
.wake-fade-enter-from,
.wake-fade-leave-to {
  opacity: 0;
}
</style>
