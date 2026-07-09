import { defineComponent, h } from 'vue';
import { createRouter, createWebHistory, RouterView } from 'vue-router';
import { useAuthStore } from './stores/authStore';
import { detectBrowserLocale } from './i18n';

const LocaleLayout = defineComponent({ render: () => h(RouterView) });

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (to.path === from.path) return false;
    if (savedPosition) return savedPosition;
    return { top: 0 };
  },
  routes: [
    { path: '/', redirect: () => `/${detectBrowserLocale()}` },
    {
      path: '/:locale(lv|en|ru)',
      component: LocaleLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('./views/Home/HomeView.vue'),
        },
        {
          path: 'listing/:id',
          name: 'listing',
          component: () => import('./views/ListingDetail/ListingView.vue'),
          props: true,
        },
        {
          path: 'add-listing',
          name: 'add-listing',
          component: () => import('./views/ListingForm/AddListingView.vue'),
        },
        {
          path: 'listing/:id/edit',
          name: 'edit-listing',
          component: () => import('./views/ListingForm/EditListingView.vue'),
          props: true,
        },
        {
          path: 'property/:id/add-listing',
          name: 'add-listing-to-property',
          component: () =>
            import('./views/ListingForm/AddListingToPropertyView.vue'),
          props: true,
        },
        {
          path: 'verify-email',
          name: 'verify-email',
          component: () => import('./views/VerifyEmailView.vue'),
        },
        {
          path: 'reset-password',
          name: 'reset-password',
          component: () => import('./views/ResetPasswordView.vue'),
        },
        {
          path: 'privacy',
          name: 'privacy',
          component: () => import('./views/PrivacyPolicyView.vue'),
        },
        {
          path: 'terms',
          name: 'terms',
          component: () => import('./views/TermsView.vue'),
        },
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          component: () => import('./views/NotFoundView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: (to) =>
        `/${detectBrowserLocale()}${to.path === '/' ? '' : to.path}`,
    },
  ],
});

router.beforeEach(async (to) => {
  const locale = to.params.locale as string | undefined;

  if (locale === 'lv' || locale === 'en' || locale === 'ru') {
    const { setLocale } = await import('./i18n');
    setLocale(locale);
    document.documentElement.lang = locale;
  }

  const auth = useAuthStore();
  const needsAuth =
    to.name === 'add-listing' ||
    to.name === 'edit-listing' ||
    to.name === 'add-listing-to-property';
  if (needsAuth) {
    // The app now mounts before session restoration finishes, so wait for it
    // before deciding whether to bounce an authenticated user off a guarded route.
    await auth.init();
    if (!auth.isAuthenticated) {
      return { name: 'home', params: { locale: locale ?? 'lv' } };
    }
  }
});
