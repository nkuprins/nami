<script setup lang="ts">
import { ref, watch } from 'vue';
import { renderMarkdown } from '../utils/renderMarkdown';
import { useLocaleRoute } from '../composables/useLocaleRoute';

const { locale } = useLocaleRoute();
const html = ref('');

async function load() {
  const res = await fetch(`/docs/terms-of-use.${locale.value}.md`);
  const text = await res.text();
  html.value = renderMarkdown(text);
}

watch(locale, load, { immediate: true });
</script>

<template>
  <div class="mx-auto max-w-2xl px-4 sm:px-6 py-12 sm:py-16">
    <div class="legal-content" v-html="html" />
  </div>
</template>
