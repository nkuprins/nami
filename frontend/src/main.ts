import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router';
import { useAuthStore } from './stores/authStore';
import { logger } from './utils/logger';
import './style.css';

async function main() {
  if (import.meta.env.VITE_MOCK === 'true') {
    const { installMock } = await import('./mock/index');
    await installMock().catch((err) => {
      logger.warn(
        '[mock] MSW failed to launch. Network requests will not be mocked:',
        err
      );
    });
  }
  const app = createApp(App);
  const { default: i18n } = await import('./i18n');
  app.use(createPinia()).use(i18n).use(router);
  // Mount immediately so the shell (and the "waking up" overlay) render right
  // away. Session restoration runs in the background: on a cold backend the
  // slow /auth/me now shows the overlay instead of a blank, unmounted page.
  app.mount('#app');
  useAuthStore().init();
}

try {
  await main();
} catch (error) {
  logger.error('Failed to initialize the application:', error);
}
