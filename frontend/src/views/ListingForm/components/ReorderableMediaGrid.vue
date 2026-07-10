<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import IconChevron from '../../../components/icons/IconChevron.vue';

const { t } = useI18n();

const props = defineProps<{
  items: Array<{ preview: string } | { url: string }>;
  altText: (index: number) => string;
}>();

const emit = defineEmits<{
  (e: 'remove', index: number): void;
  (e: 'move', from: number, to: number): void;
}>();

function thumbSrc(entry: { preview: string } | { url: string }): string {
  return 'preview' in entry ? entry.preview : entry.url;
}

const dragIndex = ref<number | null>(null);
const overIndex = ref<number | null>(null);

function onDragStart(i: number, e: DragEvent) {
  dragIndex.value = i;
  // Required for the drag to actually start in Firefox.
  e.dataTransfer?.setData('text/plain', String(i));
  if (e.dataTransfer) e.dataTransfer.effectAllowed = 'move';
}

function onDrop(i: number) {
  if (dragIndex.value !== null) emit('move', dragIndex.value, i);
  dragIndex.value = null;
  overIndex.value = null;
}

function onDragEnd() {
  dragIndex.value = null;
  overIndex.value = null;
}
</script>

<template>
  <div class="grid grid-cols-3 gap-2 mt-1">
    <div
      v-for="(entry, i) in props.items"
      :key="i"
      draggable="true"
      class="relative cursor-grab active:cursor-grabbing transition-opacity"
      :class="{
        'opacity-40': dragIndex === i,
        'ring-2 ring-accent rounded-lg': overIndex === i && dragIndex !== i,
      }"
      @dragstart="onDragStart(i, $event)"
      @dragenter.prevent="overIndex = i"
      @dragover.prevent
      @drop="onDrop(i)"
      @dragend="onDragEnd"
    >
      <img
        :src="thumbSrc(entry)"
        draggable="false"
        class="w-full aspect-square object-cover rounded-lg"
        :alt="props.altText(i)"
      />
      <slot name="badge" :index="i" />
      <button
        type="button"
        class="absolute top-1 right-1 size-5 rounded-full bg-ink/70 text-bg text-xs flex items-center justify-center hover:bg-ink transition-colors"
        @click="emit('remove', i)"
      >
        ✕
      </button>
      <div class="absolute bottom-1 left-1 right-1 flex justify-between">
        <button
          type="button"
          class="size-5 rounded-full bg-ink/70 text-bg flex items-center justify-center hover:bg-ink transition-colors disabled:opacity-30"
          :disabled="i === 0"
          :aria-label="t('addListing.moveLeft')"
          @click="emit('move', i, i - 1)"
        >
          <IconChevron dir="left" class="size-3" />
        </button>
        <button
          type="button"
          class="size-5 rounded-full bg-ink/70 text-bg flex items-center justify-center hover:bg-ink transition-colors disabled:opacity-30"
          :disabled="i === props.items.length - 1"
          :aria-label="t('addListing.moveRight')"
          @click="emit('move', i, i + 1)"
        >
          <IconChevron dir="right" class="size-3" />
        </button>
      </div>
    </div>
  </div>
</template>
