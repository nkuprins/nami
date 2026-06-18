import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from './stores/authStore';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('./views/HomeView.vue'),
    },
    {
      path: '/property/:id',
      name: 'property',
      component: () => import('./views/PropertyView.vue'),
      props: true,
    },
    {
      path: '/add-property',
      name: 'add-property',
      component: () => import('./views/AddPropertyView.vue'),
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
