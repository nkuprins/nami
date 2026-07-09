import { logger } from '../utils/logger';
import { trackWake } from '../composables/useWakeStatus';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '';

let refreshPromise: Promise<boolean> | null = null;

const MUTATING_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

function readCookie(name: string): string | null {
  const match = document.cookie.match(
    new RegExp('(?:^|; )' + name + '=([^;]*)')
  );
  return match ? decodeURIComponent(match[1]) : null;
}

/**
 * Echoes Spring's readable `XSRF-TOKEN` cookie back in the `X-XSRF-TOKEN`
 * header for state-changing requests, satisfying the server's CSRF check.
 * Safe methods and requests made before the token cookie exists pass through
 * untouched.
 */
function withCsrf(init?: RequestInit): RequestInit | undefined {
  const method = (init?.method ?? 'GET').toUpperCase();
  if (!MUTATING_METHODS.has(method)) return init;

  const token = readCookie('XSRF-TOKEN');
  if (!token) return init;

  const headers = new Headers(init?.headers);
  headers.set('X-XSRF-TOKEN', token);
  return { ...init, headers };
}

/**
 * Enhanced fetch wrapper with automatic token refresh. Wrapped in
 * {@link trackWake} so slow cold-start requests surface a "waking up" overlay.
 */
export function fetchApi(input: string, init?: RequestInit): Promise<Response> {
  return trackWake(fetchApiInner(input, init));
}

async function fetchApiInner(
  input: string,
  init?: RequestInit
): Promise<Response> {
  const res = await fetch(API_BASE + input, withCsrf(init));
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
  return fetch(API_BASE + input, withCsrf(init));
}
