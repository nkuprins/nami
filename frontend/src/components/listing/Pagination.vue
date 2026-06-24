<script setup lang="ts">
import { computed } from 'vue';
import IconChevron from '../icons/IconChevron.vue';

const props = defineProps<{
  page: number;
  pageCount: number;
}>();
const emit = defineEmits<{ change: [page: number] }>();

const pages = computed<Array<number | '…'>>(() => {
  const total = props.pageCount;
  const cur = props.page;
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1);
  const out: Array<number | '…'> = [1];
  const start = Math.max(2, cur - 1);
  const end = Math.min(total - 1, cur + 1);
  if (start > 2) out.push('…');
  for (let i = start; i <= end; i++) out.push(i);
  if (end < total - 1) out.push('…');
  out.push(total);
  return out;
});

function go(p: number) {
  if (p < 1 || p > props.pageCount || p === props.page) return;
  emit('change', p);
}
</script>

<template>
  <nav
    v-if="pageCount > 1"
    class="flex items-center justify-center gap-1 pt-6 mt-6 pb-6 mb-6 border-t border-line"
  >
    <button
      type="button"
      @click="go(page - 1)"
      :disabled="page === 1"
      class="focus-ring size-10 grid place-items-center rounded-md text-ink-2 hover:text-ink hover:bg-surface disabled:opacity-30 transition-colors"
    >
      <span class="size-4"><IconChevron dir="left" /></span>
    </button>

    <template v-for="(p, i) in pages" :key="i">
      <span
        v-if="p === '…'"
        class="px-1 text-ink-3 select-none"
        aria-hidden="true"
        >…</span
      >
      <button
        v-else
        type="button"
        @click="go(p)"
        :aria-current="p === page ? 'page' : undefined"
        class="focus-ring size-10 grid place-items-center rounded-md tabular text-sm transition-colors"
        :class="
          p === page
            ? 'bg-ink text-bg'
            : 'text-ink-2 hover:text-ink hover:bg-surface'
        "
      >
        {{ p }}
      </button>
    </template>

    <button
      type="button"
      @click="go(page + 1)"
      :disabled="page === pageCount"
      class="focus-ring size-10 grid place-items-center rounded-md text-ink-2 hover:text-ink hover:bg-surface disabled:opacity-30 transition-colors"
    >
      <span class="size-4"><IconChevron dir="right" /></span>
    </button>
  </nav>
</template>
