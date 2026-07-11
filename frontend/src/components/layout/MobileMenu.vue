<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import IconClose from '../icons/IconClose.vue';

const { t } = useI18n();

defineProps<{
  open: boolean;
  isAuthenticated: boolean;
  savedCount: number;
}>();

const emit = defineEmits<{
  'update:open': [value: boolean];
  'add-property': [];
  'my-listings': [];
  saved: [];
  'sign-in': [];
  'sign-out': [];
  'edit-profile': [];
  'export-data': [];
  'delete-account': [];
}>();
</script>

<template>
  <Teleport to="body">
    <transition name="scrim">
      <div
        v-if="open"
        class="sm:hidden fixed inset-0 z-50 bg-ink/45 backdrop-blur-sm"
        @click="emit('update:open', false)"
      />
    </transition>

    <transition name="drawer-right">
      <aside
        v-if="open"
        class="sm:hidden fixed inset-y-0 right-0 w-72 z-50 bg-bg shadow-lift flex flex-col"
      >
        <header
          class="flex items-center justify-between px-5 h-14 border-b border-line"
        >
          <span class="micro-label text-ink-3">{{ t('nav.menu') }}</span>
          <button
            class="focus-ring size-10 -mr-2 grid place-items-center text-ink-2 hover:text-ink"
            @click="emit('update:open', false)"
          >
            <span class="size-5"><IconClose /></span>
          </button>
        </header>

        <nav class="flex-1 flex flex-col px-3 py-4 gap-1">
          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
            @click="emit('my-listings')"
          >
            {{ t('nav.myListings') }}
          </button>

          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
            @click="emit('saved')"
          >
            {{ t('nav.saved') }}
            <span
              v-if="savedCount > 0"
              class="tabular text-xs font-medium px-2 h-5 inline-flex items-center rounded-full bg-surface text-ink"
            >
              {{ savedCount }}
            </span>
          </button>

          <template v-if="isAuthenticated">
            <button
              class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
              @click="emit('edit-profile')"
            >
              {{ t('auth.editProfile') }}
            </button>
            <button
              class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
              @click="emit('sign-out')"
            >
              {{ t('nav.signOut') }}
            </button>
          </template>
          <template v-else>
            <button
              class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
              @click="emit('sign-in')"
            >
              {{ t('nav.signIn') }}
            </button>
          </template>

          <div class="border-t border-line my-2" />

          <button
            class="focus-ring flex items-center justify-center h-12 px-4 rounded-full text-[0.9375rem] bg-ink text-bg hover:bg-accent-2 transition-colors"
            @click="emit('add-property')"
          >
            + {{ t('nav.addProperty') }}
          </button>
        </nav>

        <footer
          v-if="isAuthenticated"
          class="border-t border-line px-3 py-3 flex flex-col gap-1"
        >
          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink-2 hover:text-ink hover:bg-surface transition-colors text-left w-full"
            @click="emit('export-data')"
          >
            {{ t('nav.exportData') }}
          </button>
          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-warn/70 hover:text-warn hover:bg-warn/5 transition-colors text-left w-full"
            @click="emit('delete-account')"
          >
            {{ t('nav.deleteAccount') }}
          </button>
        </footer>
      </aside>
    </transition>
  </Teleport>
</template>
