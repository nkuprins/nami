<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/authStore';
import { authApi } from '../../api/authApi';
import ConfirmDialog from '../ui/ConfirmDialog.vue';

defineProps<{ open: boolean }>();
const emit = defineEmits<{ 'update:open': [value: boolean] }>();

const auth = useAuthStore();
const router = useRouter();

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
    await router.push('/');
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
    title="Delete your account?"
    description="This will permanently remove your account, all your listings, and saved properties. This cannot be undone."
    confirm-label="Yes, delete my account"
    danger
    @update:open="emit('update:open', $event)"
    @confirm="handleConfirm"
  >
    <p v-if="error" class="text-xs text-warn mt-1">
      Something went wrong. Try again.
    </p>
    <p v-if="loading" class="text-xs text-ink-3 mt-1">Deleting account…</p>
  </ConfirmDialog>
</template>
