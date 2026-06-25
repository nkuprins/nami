import { defineComponent, h } from 'vue';
import { createRouter, createWebHistory, RouterView } from 'vue-router';
import { useAuthStore } from './stores/authStore';

const LocaleLayout = defineComponent({ render: () => h(RouterView) });

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (to.path === from.path) return false;
    if (savedPosition) return savedPosition;
    return { top: 0 };
  },
  routes: [
    { path: '/', redirect: '/lv' },
    {
      path: '/:locale(lv|en)',
      component: LocaleLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('./views/Home/HomeView.vue'),
        },
        {
          path: 'property/:id',
          name: 'property',
          component: () => import('./views/PropertyDetail/PropertyView.vue'),
          props: true,
        },
        {
          path: 'add-property',
          name: 'add-property',
          component: () => import('./views/AddProperty/AddPropertyView.vue'),
        },
        {
          path: '/property/:id/edit',
          name: 'edit-property',
          component: () => import('./views/AddProperty/AddPropertyView.vue'),
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
      redirect: (to) => `/lv${to.path === '/' ? '' : to.path}`,
    },
  ],
});

router.beforeEach(async (to) => {
  const locale = to.params.locale as string | undefined;

  if (locale === 'lv' || locale === 'en') {
    const { setLocale } = await import('./i18n');
    setLocale(locale);
    document.documentElement.lang = locale;
  }

  const auth = useAuthStore();
  if (
    (to.name === 'add-property' || to.name === 'edit-property') &&
    !auth.isAuthenticated
  ) {
    return { name: 'home', params: { locale: locale ?? 'lv' } };
  }
});
