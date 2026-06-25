<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/authStore';
import { useLocaleRoute } from '../composables/useLocaleRoute';
import IconFail from '../components/icons/IconFail.vue';
import IconCheck from '../components/icons/IconCheck.vue';

const { t } = useI18n();
const { localePath } = useLocaleRoute();
const route = useRoute();
const router = useRouter();
const { verifyEmail } = useAuthStore();

const status = ref<'loading' | 'success' | 'error'>('loading');
const errorMsg = ref('');

onMounted(async () => {
  const token = route.query.token as string | undefined;
  if (!token) {
    status.value = 'error';
    errorMsg.value = t('verifyEmail.noToken');
    return;
  }
  const err = await verifyEmail(token);
  void router.replace({ query: {} });
  if (err) {
    status.value = 'error';
    errorMsg.value = err;
  } else {
    status.value = 'success';
  }
});
</script>

<template>
  <div class="max-w-md mx-auto px-4 py-20 text-center">
    <p v-if="status === 'loading'" class="text-sm text-ink-2">
      {{ t('verifyEmail.verifying') }}
    </p>

    <div
      v-else-if="status === 'success'"
      class="flex flex-col items-center gap-4"
    >
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconCheck />
      </div>
      <h1 class="text-lg font-medium text-ink">
        {{ t('verifyEmail.verified') }}
      </h1>
      <p class="text-sm text-ink-2">{{ t('verifyEmail.verifiedDesc') }}</p>
      <RouterLink
        :to="localePath('/')"
        class="text-sm text-ink underline underline-offset-2"
      >
        {{ t('verifyEmail.goHome') }}
      </RouterLink>
    </div>

    <div v-else class="flex flex-col items-center gap-4">
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconFail />
      </div>
      <h1 class="text-lg font-medium text-ink">
        {{ t('verifyEmail.failed') }}
      </h1>
      <p class="text-sm text-ink-2">{{ errorMsg }}</p>
      <RouterLink
        :to="localePath('/')"
        class="text-sm text-ink underline underline-offset-2"
      >
        {{ t('verifyEmail.goHome') }}
      </RouterLink>
    </div>
  </div>
</template>
