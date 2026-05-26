<script setup lang="ts">
import {ref} from 'vue';
import IconHeart from "../ui/IconHeart.vue";

const props = defineProps<{ propertyId: string }>();
const saved = ref(false);

function handle(e: Event) {
  e.preventDefault();
  e.stopPropagation();
  saved.value = !saved.value;
}
</script>

<template>
  <button
      type="button"
      @click="handle"
      :aria-pressed="saved"
      :aria-label="saved ? 'Remove from saved' : 'Save listing'"
      class="focus-ring relative size-9 grid place-items-center rounded-full
           bg-bg/90 backdrop-blur transition-all duration-200
           hover:bg-bg hover:scale-105 active:scale-95"
      :class="{
      'text-accent-2': saved,
      'text-ink-2': !saved,
    }"
  >
    <span class="size-4 inline-block transition-transform duration-200"
          :class="{ 'scale-110': saved }">
      <IconHeart :filled="saved"/>
    </span>
  </button>
</template>