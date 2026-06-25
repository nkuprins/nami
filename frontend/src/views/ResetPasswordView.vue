<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MIN_PASSWORD_LENGTH } from '../api/authApi';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../stores/authStore';
import { useLocaleRoute } from '../composables/useLocaleRoute';
import IconCheck from '../components/icons/IconCheck.vue';
import FormField from '../components/ui/FormField.vue';

const { t } = useI18n();
const { localePath } = useLocaleRoute();
const route = useRoute();
const router = useRouter();
const { resetPassword } = useAuthStore();

const password = ref('');
const confirm = ref('');
const error = ref('');
const submitting = ref(false);
const done = ref(false);

const token = route.query.token as string | undefined;

async function submit() {
  error.value = '';
  if (!password.value) {
    error.value = t('resetPassword.enterPassword');
    return;
  }
  if (password.value.length < MIN_PASSWORD_LENGTH) {
    error.value = t('auth.passwordLength');
    return;
  }
  if (password.value !== confirm.value) {
    error.value = t('auth.passwordsMismatch');
    return;
  }
  if (!token) {
    error.value = t('auth.invalidResetLink');
    return;
  }

  submitting.value = true;
  const err = await resetPassword(token, password.value);
  submitting.value = false;

  if (err) {
    error.value = err;
    return;
  }
  done.value = true;
  void router.replace({ query: {} });
}
</script>

<template>
  <div class="max-w-md mx-auto px-4 py-20">
    <div v-if="done" class="text-center flex flex-col items-center gap-4">
      <div
        class="size-12 rounded-full bg-surface border border-line flex items-center justify-center"
      >
        <IconCheck />
      </div>
      <h1 class="text-lg font-medium text-ink">
        {{ t('resetPassword.updated') }}
      </h1>
      <p class="text-sm text-ink-2">{{ t('resetPassword.updatedDesc') }}</p>
      <RouterLink
        :to="localePath('/')"
        class="text-sm text-ink underline underline-offset-2"
      >
        {{ t('resetPassword.goHome') }}
      </RouterLink>
    </div>

    <div v-else>
      <h1 class="text-lg font-medium text-ink mb-6">
        {{ t('resetPassword.setNew') }}
      </h1>
      <form class="flex flex-col gap-4" @submit.prevent="submit">
        <FormField
          id="rp-password"
          :label="t('resetPassword.newPassword')"
          v-model="password"
          type="password"
          autocomplete="new-password"
          placeholder="••••••••••••••••"
        />
        <FormField
          id="rp-confirm"
          :label="t('resetPassword.confirmPassword')"
          v-model="confirm"
          type="password"
          autocomplete="new-password"
          placeholder="••••••••••••••••"
        />
        <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
        <button
          type="submit"
          :disabled="submitting"
          class="h-10 rounded-lg bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
        >
          {{ t('resetPassword.update') }}
        </button>
      </form>
    </div>
  </div>
</template>
