<script setup lang="ts">
import {ref} from 'vue';
import IconChevron from '../ui/IconChevron.vue';
import Popover from '../ui/Popover.vue';

defineProps<{
  label: string;
  summary?: string;
  active?: boolean;
  title?: string;
  align?: 'start' | 'center' | 'end';
  width?: number;
}>();

const open = ref(false);
const anchor = ref<HTMLButtonElement | null>(null);

function toggle() {
  open.value = !open.value;
}
</script>

<template>
  <div class="relative">
    <button
        ref="anchor"
        type="button"
        @click="toggle"
        class="focus-ring w-full inline-flex items-center justify-between gap-2 h-12 px-4
             rounded-md border bg-bg text-left transition-colors"
        :class="{
          'border-ink text-ink': open,
          'border-ink/50 text-ink': active && !open,
          'border-line hover:border-line-2': !open && !active,
        }"
    >
      <span class="flex flex-col leading-tight min-w-0">
        <span class="micro-label">{{ label }}</span>
        <span class="text-sm truncate"
              :class="summary ? 'text-ink' : 'text-ink-3'">
          {{ summary || 'Any' }}
        </span>
      </span>
      <span class="size-4 text-ink-2 shrink-0">
        <IconChevron :dir="open ? 'up' : 'down'"/>
      </span>
    </button>
    <Popover
        :open="open"
        :anchor-el="anchor"
        :title="title ?? label"
        :align="align"
        :width="width"
        @update:open="(v) => (open = v)"
    >
      <slot/>
    </Popover>
  </div>
</template>