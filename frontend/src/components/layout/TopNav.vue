<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import IconHeart from '../icons/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { useAuthStore } from '../../stores/authStore';
import AuthModal from '../auth/AuthModal.vue';
import SavedDrawer from '../listing/SavedDrawer.vue';
import MyPropertiesDrawer from '../listing/MyPropertiesDrawer.vue';
import DeleteAccountDialog from '../auth/DeleteAccountDialog.vue';
import EditProfileDialog from '../auth/EditProfileDialog.vue';
import MobileMenu from './MobileMenu.vue';
import IconBuilding from '../icons/IconBuilding.vue';
import IconChevron from '../icons/IconChevron.vue';
import IconMenu from '../icons/IconMenu.vue';
import IconClose from '../icons/IconClose.vue';
import IconUser from '../icons/IconUser.vue';
import { useLocaleRoute } from '../../composables/useLocaleRoute';
import { LOCALES } from '../../i18n';
import { authApi } from '../../api/authApi';

const { t } = useI18n();
const { locale, localePath, localePush, switchLocalePath } = useLocaleRoute();
const savedStore = useSavedStore();
const auth = useAuthStore();

const authOpen = ref(false);
const savedOpen = ref(false);
const myPropertiesOpen = ref(false);
const deleteAccountOpen = ref(false);
const editProfileOpen = ref(false);
const userMenuOpen = ref(false);
const userMenuRef = ref<HTMLElement | null>(null);

function onOutsideClick(e: MouseEvent) {
  if (!userMenuRef.value?.contains(e.target as Node)) {
    userMenuOpen.value = false;
  }
}

watch(userMenuOpen, (open) => {
  if (open) {
    document.addEventListener('click', onOutsideClick);
  } else {
    document.removeEventListener('click', onOutsideClick);
  }
});
const mobileMenuOpen = ref(false);
const langMenuOpen = ref(false);
const mobileLangOpen = ref(false);

function handleAddProperty() {
  mobileMenuOpen.value = false;
  if (auth.isAuthenticated) {
    localePush('/add-listing');
  } else {
    authOpen.value = true;
  }
}

function handleMyListings() {
  mobileMenuOpen.value = false;
  if (auth.isAuthenticated) {
    myPropertiesOpen.value = true;
  } else {
    authOpen.value = true;
  }
}

function handleSaved() {
  mobileMenuOpen.value = false;
  savedOpen.value = true;
}

function handleSignIn() {
  mobileMenuOpen.value = false;
  authOpen.value = true;
}

function handleSignOut() {
  mobileMenuOpen.value = false;
  auth.logout();
}

function handleDeleteAccount() {
  mobileMenuOpen.value = false;
  deleteAccountOpen.value = true;
}

function handleEditProfile() {
  mobileMenuOpen.value = false;
  editProfileOpen.value = true;
}

async function handleExportData() {
  const data = await authApi.exportData();
  if (!data) return;
  const blob = new Blob([JSON.stringify(data, null, 2)], {
    type: 'application/json',
  });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'baltnami-export.json';
  a.click();
  URL.revokeObjectURL(url);
}
</script>

<template>
  <header class="sticky top-0 z-40 bg-bg/95 backdrop-blur border-b border-line">
    <div
      class="mx-auto max-w-360 px-4 sm:px-6 lg:px-10 h-14 sm:h-16 flex items-center justify-between"
    >
      <RouterLink
        :to="localePath('/')"
        class="focus-ring flex flex-col group shrink-0"
      >
        <span
          class="display-headline text-[1.5rem] sm:text-[1.6rem] leading-none text-ink"
          style="
            font-variation-settings:
              'opsz' 96,
              'SOFT' 30,
              'WONK' 1;
          "
        >
          Baltnami
        </span>
        <span
          class="micro-label text-[0.55rem] tracking-[0.15em] text-ink-3 mt-0.5 hidden sm:block"
          >Est. 2026</span
        >
      </RouterLink>

      <!-- Desktop nav -->
      <nav class="hidden sm:flex items-center gap-1">
        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
          @click="
            auth.isAuthenticated ? (myPropertiesOpen = true) : (authOpen = true)
          "
        >
          <span class="size-4"><IconBuilding /></span>
          <span>{{ t('nav.myListings') }}</span>
        </button>

        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
          @click="savedOpen = true"
        >
          <span
            class="size-4"
            :class="{ 'text-accent-2': savedStore.count > 0 }"
          >
            <IconHeart :filled="savedStore.count > 0" />
          </span>
          <span>{{ t('nav.saved') }}</span>
          <span v-if="savedStore.count > 0" class="tabular text-ink">
            ({{ savedStore.count }})
          </span>
        </button>

        <div
          class="relative"
          @mouseenter="langMenuOpen = true"
          @mouseleave="langMenuOpen = false"
        >
          <button
            class="focus-ring inline-flex items-center gap-1 h-9 px-2.5 rounded-full text-xs font-semibold uppercase tracking-wide text-ink-2 hover:text-ink hover:bg-surface transition-colors"
            @click="langMenuOpen = !langMenuOpen"
          >
            {{ locale }}
            <span class="size-3 text-ink-3"
              ><IconChevron :dir="langMenuOpen ? 'up' : 'down'"
            /></span>
          </button>
          <div class="absolute left-0 right-0 h-3 top-full" />
          <Transition name="pop">
            <div
              v-if="langMenuOpen"
              class="absolute right-0 top-full mt-3 w-24 bg-bg border border-line rounded-xl shadow-lift overflow-hidden z-50"
            >
              <RouterLink
                v-for="l in LOCALES"
                :key="l"
                :to="switchLocalePath(l)"
                class="block px-4 py-2.5 text-xs font-semibold uppercase tracking-wide transition-colors"
                :class="
                  locale === l
                    ? 'text-ink bg-surface'
                    : 'text-ink-2 hover:text-ink hover:bg-surface'
                "
                @click="langMenuOpen = false"
              >
                {{ l }}
              </RouterLink>
            </div>
          </Transition>
        </div>

        <!-- Authenticated: user dropdown -->
        <template v-if="auth.isAuthenticated">
          <div ref="userMenuRef" class="relative">
            <button
              class="focus-ring inline-flex items-center gap-1.5 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
              @click="userMenuOpen = !userMenuOpen"
            >
              <span class="max-w-32 truncate">{{ auth.user?.name }}</span>
              <span class="size-4 text-ink-3">
                <IconChevron :dir="userMenuOpen ? 'up' : 'down'" />
              </span>
            </button>

            <div class="absolute left-0 right-0 h-3 top-full" />

            <Transition name="pop">
              <div
                v-if="userMenuOpen"
                class="absolute right-0 top-full mt-3 w-48 bg-bg border border-line rounded-xl shadow-lift overflow-hidden z-50"
              >
                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-ink hover:bg-surface transition-colors"
                  @click="
                    editProfileOpen = true;
                    userMenuOpen = false;
                  "
                >
                  {{ t('auth.editProfile') }}
                </button>
                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-ink-2 hover:bg-surface transition-colors"
                  @click="
                    handleExportData();
                    userMenuOpen = false;
                  "
                >
                  {{ t('nav.exportData') }}
                </button>
                <div class="border-t border-line" />
                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-ink hover:bg-surface transition-colors"
                  @click="
                    auth.logout();
                    userMenuOpen = false;
                  "
                >
                  {{ t('nav.signOut') }}
                </button>
              </div>
            </Transition>
          </div>
        </template>

        <button
          v-else
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
          @click="authOpen = true"
        >
          <span class="size-4"><IconUser /></span>
          {{ t('nav.signIn') }}
        </button>

        <button
          class="focus-ring inline-flex items-center h-9 px-5 ml-2 rounded-full text-sm bg-ink text-bg hover:bg-accent-2 transition-colors"
          @click="handleAddProperty"
        >
          <span class="hidden md:inline">{{ t('nav.addProperty') }}</span>
        </button>
      </nav>

      <!-- Mobile nav -->
      <div class="flex sm:hidden items-center gap-2">
        <div class="relative">
          <button
            class="focus-ring inline-flex items-center gap-0.5 h-10 px-2 rounded-full text-xs font-semibold uppercase tracking-wide text-ink-2 hover:text-ink hover:bg-surface transition-colors"
            @click="mobileLangOpen = !mobileLangOpen"
          >
            {{ locale }}
            <span class="size-3 text-ink-3"
              ><IconChevron :dir="mobileLangOpen ? 'up' : 'down'"
            /></span>
          </button>
          <Transition name="pop">
            <div
              v-if="mobileLangOpen"
              class="absolute right-0 top-full mt-1 w-20 bg-bg border border-line rounded-xl shadow-lift overflow-hidden z-50"
            >
              <RouterLink
                v-for="l in LOCALES"
                :key="l"
                :to="switchLocalePath(l)"
                class="block px-3 py-2 text-xs font-semibold uppercase tracking-wide transition-colors"
                :class="
                  locale === l
                    ? 'text-ink bg-surface'
                    : 'text-ink-2 hover:text-ink hover:bg-surface'
                "
                @click="mobileLangOpen = false"
              >
                {{ l }}
              </RouterLink>
            </div>
          </Transition>
        </div>

        <button
          class="focus-ring relative size-10 grid place-items-center rounded-full hover:text-ink transition-colors"
          :class="savedStore.count > 0 ? 'text-accent-2' : 'text-ink-2'"
          @click="savedOpen = true"
        >
          <span class="size-5"
            ><IconHeart :filled="savedStore.count > 0"
          /></span>
          <span
            v-if="savedStore.count > 0"
            class="absolute top-0.5 right-0.5 tabular text-[0.625rem] font-semibold min-w-4 h-4 flex items-center justify-center rounded-full bg-ink text-cream"
          >
            {{ savedStore.count }}
          </span>
        </button>

        <button
          class="focus-ring size-10 grid place-items-center rounded-full text-ink-2 hover:text-ink transition-colors"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <span class="size-5">
            <IconMenu v-if="!mobileMenuOpen" />
            <IconClose v-else />
          </span>
        </button>
      </div>
    </div>
  </header>

  <MobileMenu
    :open="mobileMenuOpen"
    :is-authenticated="auth.isAuthenticated"
    :saved-count="savedStore.count"
    @update:open="mobileMenuOpen = $event"
    @add-property="handleAddProperty"
    @my-listings="handleMyListings"
    @saved="handleSaved"
    @sign-in="handleSignIn"
    @sign-out="handleSignOut"
    @edit-profile="handleEditProfile"
    @export-data="handleExportData"
    @delete-account="handleDeleteAccount"
  />

  <AuthModal v-model:open="authOpen" />
  <SavedDrawer v-model:open="savedOpen" />
  <MyPropertiesDrawer v-model:open="myPropertiesOpen" />
  <EditProfileDialog
    v-model:open="editProfileOpen"
    @delete-account="
      editProfileOpen = false;
      deleteAccountOpen = true;
    "
  />
  <DeleteAccountDialog v-model:open="deleteAccountOpen" />
</template>
