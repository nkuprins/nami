import {setupWorker} from 'msw/browser';
import {handlers} from './handlers';
import {logger} from "../utils/logger";

export const worker = setupWorker(...handlers);

export function installMock(): Promise<ServiceWorkerRegistration | undefined> {
    logger.info('[mock] MSW Service Worker installing...');
    return worker.start({
        onUnhandledRequest: 'bypass',
    });
}
