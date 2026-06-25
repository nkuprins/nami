<script setup lang="ts">
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useAuthStore } from '../../stores/authStore';
import { authApi } from '../../api/authApi';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import ConfirmDialog from '../ui/ConfirmDialog.vue';

const { t } = useI18n();
const { localePush } = useLocaleRoute();

defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const auth = useAuthStore();

const loading = ref(false);
const error = ref(false);

async function deleteAccount() {
  await authApi.deleteAccount();
}

async function handleConfirm() {
  loading.value = true;
  error.value = false;
  try {
    await deleteAccount();
    await auth.logout();
    emit('update:open', false);
    await localePush('/');
  } catch {
    error.value = true;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <ConfirmDialog
    :open="open"
    :title="t('auth.deleteAccount')"
    :description="t('auth.deleteAccountDesc')"
    :confirm-label="t('auth.confirmDeleteAccount')"
    danger
    @update:open="emit('update:open', $event)"
    @confirm="handleConfirm"
  >
    <p v-if="error" class="text-xs text-warn mt-1">
      {{ t('auth.somethingWentWrong') }}
    </p>
    <p v-if="loading" class="text-xs text-ink-3 mt-1">
      {{ t('auth.deletingAccount') }}
    </p>
  </ConfirmDialog>
</template>
