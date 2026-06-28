<script setup lang="ts">
import { RouterLink } from 'vue-router';
import { formatPrice } from '../../utils/format';

defineProps<{
  id: string;
  title: string;
  district: string;
  city: string;
  price: number;
  type: string;
  photo: string | undefined;
}>();

const emit = defineEmits<{
  navigate: [];
}>();
</script>

<template>
  <div
    class="flex flex-col rounded-xl border border-line overflow-hidden hover:border-ink/30 transition-colors"
  >
    <div class="flex min-h-20 sm:min-h-24">
      <RouterLink
        :to="`/property/${id}`"
        class="flex flex-1 min-w-0 gap-3"
        @click="emit('navigate')"
      >
        <div
          class="shrink-0 w-24 sm:w-32 self-stretch overflow-hidden bg-surface"
        >
          <img
            v-if="photo"
            :src="photo"
            :alt="title"
            class="w-full h-full object-cover"
          />
        </div>

        <div
          class="flex-1 min-w-0 py-3 sm:py-4 pr-2 flex flex-col justify-between"
        >
          <div>
            <p class="text-sm sm:text-base font-semibold text-ink line-clamp-1">
              {{ title }}
            </p>
            <p class="text-xs text-ink-2 mt-0.5 line-clamp-1">
              {{ district }}, {{ city }}
            </p>
            <div class="min-w-0 overflow-hidden mt-0.5">
              <slot name="subtitle" />
            </div>
          </div>
          <p class="text-sm sm:text-lg font-bold text-ink">
            {{ formatPrice(price, type) }}
          </p>
        </div>
      </RouterLink>

      <slot name="action" />
    </div>

    <div v-if="$slots.footer" class="border-t border-line">
      <slot name="footer" />
    </div>
  </div>
</template>
