import {createApp} from 'vue';
import {createPinia} from 'pinia';
import App from './App.vue';
import {router} from './router';
import {useAuthStore} from './stores/authStore';
import {logger} from './utils/logger';
import './style.css';

async function main() {
  if (import.meta.env.VITE_MOCK === 'true') {
      const {installMock} = await import('./mock/index');
    await installMock().catch((err) => {
      logger.warn(
          '[mock] MSW failed to launch. Network requests will not be mocked:',
          err
      );
    });
  }
  const app = createApp(App);
  app.use(createPinia()).use(router);
  await useAuthStore().init();
  app.mount('#app');
}

main().catch((error) => {
  logger.error('Failed to initialize the application:', error);
});
