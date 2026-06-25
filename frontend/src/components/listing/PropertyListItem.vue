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
    class="flex rounded-xl border border-line overflow-hidden hover:border-ink/30 transition-colors"
  >
    <RouterLink
      :to="`/property/${id}`"
      class="flex flex-1 min-w-0 gap-3"
      @click="emit('navigate')"
    >
      <div class="shrink-0 w-24 sm:w-28 h-20 overflow-hidden bg-surface">
        <img
          v-if="photo"
          :src="photo"
          :alt="title"
          class="w-full h-full object-cover"
        />
      </div>

      <div class="flex-1 min-w-0 py-3 pr-2 flex flex-col justify-between">
        <div>
          <p class="text-sm font-medium text-ink line-clamp-1">{{ title }}</p>
          <p class="text-xs text-ink-3 mt-0.5 line-clamp-1">
            {{ district }}, {{ city }}
          </p>
        </div>
        <p class="text-sm font-semibold text-ink">
          {{ formatPrice(price, type) }}
        </p>
      </div>
    </RouterLink>

    <slot name="action" />
  </div>
</template>
