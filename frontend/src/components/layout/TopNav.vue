<script setup lang="ts">
import { ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import IconHeart from '../icons/IconHeart.vue';
import { useSavedStore } from '../../stores/savedStore';
import { useAuthStore } from '../../stores/authStore';
import AuthModal from '../auth/AuthModal.vue';
import SavedDrawer from '../listing/SavedDrawer.vue';
import MyPropertiesDrawer from '../listing/MyPropertiesDrawer.vue';
import DeleteAccountDialog from '../auth/DeleteAccountDialog.vue';
import IconBuilding from '../icons/IconBuilding.vue';
import IconChevron from '../icons/IconChevron.vue';
import IconMenu from '../icons/IconMenu.vue';
import IconClose from '../icons/IconClose.vue';
import IconUser from '../icons/IconUser.vue';

const router = useRouter();
const savedStore = useSavedStore();
const auth = useAuthStore();

const authOpen = ref(false);
const savedOpen = ref(false);
const myPropertiesOpen = ref(false);
const deleteAccountOpen = ref(false);
const userMenuOpen = ref(false);
const mobileMenuOpen = ref(false);

function handleAddProperty() {
  mobileMenuOpen.value = false;
  if (auth.isAuthenticated) {
    router.push('/add-property');
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
</script>

<template>
  <header class="sticky top-0 z-40 bg-bg/95 backdrop-blur border-b border-line">
    <div
      class="mx-auto max-w-360 px-4 sm:px-6 lg:px-10 h-14 sm:h-16 flex items-center justify-between"
    >
      <!-- Logo -->
      <RouterLink to="/" class="focus-ring flex flex-col group shrink-0">
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
        <span class="micro-label text-[0.55rem] tracking-[0.15em] text-ink-3 mt-0.5 hidden sm:block">Est. 2026</span>
      </RouterLink>

      <!-- Desktop nav -->
      <nav class="hidden sm:flex items-center gap-1">
        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
          @click="auth.isAuthenticated ? (myPropertiesOpen = true) : (authOpen = true)"
        >
          <span class="size-4"><IconBuilding /></span>
          My listings
        </button>

        <button
          class="focus-ring inline-flex items-center gap-2 h-9 px-3 rounded-full text-sm text-ink-2 hover:text-ink hover:bg-surface transition-colors"
          @click="savedOpen = true"
        >
          <span class="size-4" :class="{ 'text-accent-2': savedStore.count > 0 }">
            <IconHeart :filled="savedStore.count > 0" />
          </span>
          Saved<span v-if="savedStore.count > 0" class="tabular text-ink">({{ savedStore.count }})</span>
        </button>

        <!-- Authenticated: user dropdown -->
        <template v-if="auth.isAuthenticated">
          <div
            class="relative"
            @mouseenter="userMenuOpen = true"
            @mouseleave="userMenuOpen = false"
          >
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
                  @click="auth.logout(); userMenuOpen = false"
                >
                  Sign out
                </button>
                <div class="border-t border-line" />
                <button
                  class="w-full text-left px-4 py-2.5 text-sm text-warn/70 hover:text-warn hover:bg-warn/5 transition-colors"
                  @click="deleteAccountOpen = true; userMenuOpen = false"
                >
                  Delete account
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
          Sign in
        </button>

        <button
          class="focus-ring inline-flex items-center h-9 px-5 ml-2 rounded-full text-sm bg-ink text-bg hover:bg-accent-2 transition-colors"
          @click="handleAddProperty"
        >
          + Add property
        </button>
      </nav>

      <!-- Mobile nav -->
      <div class="flex sm:hidden items-center gap-2">
        <button
          class="focus-ring relative size-10 grid place-items-center rounded-full hover:text-ink transition-colors"
          :class="savedStore.count > 0 ? 'text-accent-2' : 'text-ink-2'"
          @click="savedOpen = true"
        >
          <span class="size-5"><IconHeart :filled="savedStore.count > 0" /></span>
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

  <!-- Mobile slide-out menu -->
  <Teleport to="body">
    <transition name="scrim">
      <div
        v-if="mobileMenuOpen"
        class="sm:hidden fixed inset-0 z-50 bg-ink/45 backdrop-blur-sm"
        @click="mobileMenuOpen = false"
      />
    </transition>

    <transition name="drawer-right">
      <aside
        v-if="mobileMenuOpen"
        class="sm:hidden fixed inset-y-0 right-0 w-72 z-50 bg-bg shadow-lift flex flex-col"
      >
        <header class="flex items-center justify-between px-5 h-14 border-b border-line">
          <span class="micro-label text-ink-3">Menu</span>
          <button
            class="focus-ring size-10 -mr-2 grid place-items-center text-ink-2 hover:text-ink"
            @click="mobileMenuOpen = false"
          >
            <span class="size-5"><IconClose /></span>
          </button>
        </header>

        <nav class="flex-1 flex flex-col px-3 py-4 gap-1">
          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
            @click="handleMyListings"
          >
            My listings
          </button>

          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
            @click="handleSaved"
          >
            Saved
            <span
              v-if="savedStore.count > 0"
              class="tabular text-xs font-medium px-2 h-5 inline-flex items-center rounded-full bg-surface text-ink"
            >
              {{ savedStore.count }}
            </span>
          </button>

          <template v-if="auth.isAuthenticated">
            <button
              class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
              @click="handleSignOut"
            >
              Sign out
            </button>
          </template>
          <template v-else>
            <button
              class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-ink hover:bg-surface transition-colors text-left"
              @click="handleSignIn"
            >
              Sign in
            </button>
          </template>

          <div class="border-t border-line my-2" />

          <button
            class="focus-ring flex items-center justify-center h-12 px-4 rounded-xl text-[0.9375rem] bg-ink text-bg hover:bg-accent-2 transition-colors"
            @click="handleAddProperty"
          >
            + Add property
          </button>
        </nav>

        <footer v-if="auth.isAuthenticated" class="border-t border-line px-3 py-3">
          <button
            class="focus-ring flex items-center gap-3 h-12 px-4 rounded-xl text-[0.9375rem] text-warn/70 hover:text-warn hover:bg-warn/5 transition-colors text-left w-full"
            @click="handleDeleteAccount"
          >
            Delete account
          </button>
        </footer>
      </aside>
    </transition>
  </Teleport>

  <AuthModal v-model:open="authOpen" />
  <SavedDrawer v-model:open="savedOpen" />
  <MyPropertiesDrawer v-model:open="myPropertiesOpen" />
  <DeleteAccountDialog v-model:open="deleteAccountOpen" />
</template>
