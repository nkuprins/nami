import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from './stores/authStore';

export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition;
    if (to.path === from.path) return false;
    return { top: 0 };
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('./views/Home/HomeView.vue'),
    },
    {
      path: '/property/:id',
      name: 'property',
      component: () => import('./views/PropertyDetail/PropertyView.vue'),
      props: true,
    },
    {
      path: '/add-property',
      name: 'add-property',
      component: () => import('./views/AddProperty/AddPropertyView.vue'),
    },
    {
      path: '/verify-email',
      name: 'verify-email',
      component: () => import('./views/VerifyEmailView.vue'),
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: () => import('./views/ResetPasswordView.vue'),
    },
    {
      path: '/privacy',
      name: 'privacy',
      component: () => import('./views/PrivacyPolicyView.vue'),
    },
    {
      path: '/terms',
      name: 'terms',
      component: () => import('./views/TermsView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('./views/NotFoundView.vue'),
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.name === 'add-property' && !auth.isAuthenticated) {
    return { name: 'home' };
  }
});
