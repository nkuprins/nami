<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import Drawer from '../ui/Drawer.vue';
import FormField from '../ui/FormField.vue';
import { useAuthStore } from '../../stores/authStore';

const { t } = useI18n();

const props = defineProps<{ open: boolean }>();
const emit = defineEmits<{
  'update:open': [value: boolean];
  'delete-account': [];
}>();

const auth = useAuthStore();

const name = ref('');
const email = ref('');
const submitting = ref(false);
const emailError = ref('');
const genericError = ref('');
const successMsg = ref('');

watch(
  () => props.open,
  (val) => {
    if (val) {
      name.value = auth.user?.name ?? '';
      email.value = auth.user?.email ?? '';
      emailError.value = '';
      genericError.value = '';
      successMsg.value = '';
      submitting.value = false;
    }
  }
);

async function submit() {
  emailError.value = '';
  genericError.value = '';

  const trimmedName = name.value.trim();
  const trimmedEmail = email.value.trim();
  const nameChanged = trimmedName !== '' && trimmedName !== auth.user?.name;
  const emailChanged =
    trimmedEmail !== '' &&
    trimmedEmail.toLowerCase() !== auth.user?.email?.toLowerCase();

  if (!nameChanged && !emailChanged) {
    genericError.value = t('auth.noChanges');
    return;
  }

  submitting.value = true;
  try {
    const result = await auth.updateProfile(name.value, email.value);
    if (result.error === 'EMAIL_TAKEN') {
      emailError.value = t('auth.emailTaken');
    } else if (result.error) {
      genericError.value = t('auth.somethingWentWrong');
    } else {
      successMsg.value = result.emailChanged
        ? t('auth.checkEmailNewAddress')
        : t('auth.profileUpdated');
      setTimeout(
        () => emit('update:open', false),
        result.emailChanged ? 2500 : 1200
      );
    }
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <Drawer
    :open="open"
    :title="t('auth.editProfile')"
    @update:open="emit('update:open', $event)"
  >
    <form class="flex flex-col gap-4" @submit.prevent="submit">
      <FormField
        id="edit-name"
        :label="t('auth.fullNameLabel')"
        v-model="name"
        type="text"
        autocomplete="name"
      />
      <FormField
        id="edit-email"
        :label="t('auth.emailLabel')"
        v-model="email"
        type="email"
        autocomplete="email"
        :error="emailError"
      />
      <p v-if="successMsg" class="text-sm text-success">{{ successMsg }}</p>
      <p v-if="genericError" class="text-sm text-warn">{{ genericError }}</p>
    </form>

    <div class="mt-6 pt-4 border-t border-line">
      <button
        type="button"
        class="text-sm text-warn/60 hover:text-warn transition-colors"
        @click="
          emit('update:open', false);
          emit('delete-account');
        "
      >
        {{ t('nav.deleteAccount') }}
      </button>
    </div>
    <template #footer>
      <div class="flex gap-3">
        <button
          type="button"
          class="h-10 flex-1 rounded-full border border-line-2 text-sm font-medium text-ink-2 hover:text-ink hover:bg-surface hover:border-ink-3 transition-colors"
          @click="emit('update:open', false)"
        >
          {{ t('common.cancel') }}
        </button>
        <button
          type="button"
          :disabled="submitting"
          class="h-10 flex-1 rounded-full bg-ink text-bg text-sm font-medium hover:bg-accent-2 transition-colors disabled:opacity-50"
          @click="submit"
        >
          {{ submitting ? t('auth.saving') : t('auth.saveChanges') }}
        </button>
      </div>
    </template>
  </Drawer>
</template>
