<script setup lang="ts">
import {computed} from 'vue';
import {PropertyItem} from "../../types/propertyItem";
import {isNew} from "../../utils/format";

const props = defineProps<{ property: PropertyItem }>();
const status = computed(() => isNew(props.property.postedAt));
</script>

<template>
  <span
      v-if="status"
      class="inline-flex items-center gap-1.5 h-6 pl-1.5 pr-2.5 rounded-sm
           bg-bg/95 backdrop-blur micro-label text-ink!"
  >
    <span class="size-1.5 rounded-full bg-success" aria-hidden="true"/>
      New listing
  </span>
</template>