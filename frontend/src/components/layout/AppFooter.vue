<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useLocaleRoute } from '../../composables/useLocaleRoute';

const { t } = useI18n();
const { localePath } = useLocaleRoute();
</script>

<template>
  <footer class="border-t border-line mt-auto">
    <div
      class="mx-auto max-w-360 px-6 lg:px-10 pt-8 pb-24 lg:pb-8 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <p class="text-xs text-ink-3">
        © {{ new Date().getFullYear() }} Baltnami. {{ t('footer.allRights') }}
      </p>
      <nav class="flex items-center gap-5">
        <RouterLink
          :to="localePath('/privacy')"
          class="text-xs text-ink-3 hover:text-ink underline underline-offset-2 transition-colors"
        >
          {{ t('footer.privacy') }}
        </RouterLink>
        <RouterLink
          :to="localePath('/terms')"
          class="text-xs text-ink-3 hover:text-ink underline underline-offset-2 transition-colors"
        >
          {{ t('footer.terms') }}
        </RouterLink>
      </nav>
    </div>
  </footer>
</template>
