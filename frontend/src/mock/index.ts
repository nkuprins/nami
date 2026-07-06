import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';
import { logger } from '../utils/logger';

export const worker = setupWorker(...handlers);

export function installMock(): Promise<ServiceWorkerRegistration | undefined> {
  logger.info('[mock] MSW Service Worker installing...');
  if (import.meta.env.VITE_MOCK_AUTO_LOGIN === 'true') {
    // Lets authStore's hasSessionHint() check pass so it calls getMe() on
    // boot instead of assuming a guest, picking up the pre-seeded mock user.
    document.cookie = 'has_session=1; path=/';
  }
  return worker.start({
    onUnhandledRequest: 'bypass',
  });
}
