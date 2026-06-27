import { logger } from '../utils/logger';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

let refreshPromise: Promise<boolean> | null = null;

/**
 * Enhanced fetch wrapper with automatic token refresh
 */
export async function fetchApi(
  input: string,
  init?: RequestInit
): Promise<Response> {
  const res = await fetch(API_BASE + input, init);
  if (res.status !== 401) return res;

  logger.info(`[fetchApi] 401 Unauthorized encountered for: ${input}`);

  if (!refreshPromise) {
    logger.debug(
      '[fetchApi] No active refresh cycle found. Starting token refresh POST...'
    );

    refreshPromise = fetch(`${API_BASE}/api/auth/refresh`, { method: 'POST' })
      .then((r) => {
        logger.debug(`[fetchApi] Refresh network response status: ${r.status}`);
        return r.ok;
      })
      .catch((err) => {
        logger.error('[fetchApi] Critical error during refresh fetch:', err);
        return false;
      })
      .finally(() => {
        logger.debug('[fetchApi] Refresh cycle completed. Clearing lock.');
        refreshPromise = null;
      });
  } else {
    logger.debug(
      `[fetchApi] Refresh already in progress. Queueing request for: ${input}`
    );
  }

  const refreshed = await refreshPromise;

  if (!refreshed) {
    logger.warn(
      `[fetchApi] Refresh rejected. Session invalid. Dispatching logout event.`
    );
    window.dispatchEvent(new Event('auth:logout'));
    return res;
  }

  logger.info(
    `[fetchApi] Refresh successful. Retrying original request: ${input}`
  );
  return fetch(API_BASE + input, init);
}
