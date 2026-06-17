let refreshPromise: Promise<boolean> | null = null;

export async function fetchApi(input: string, init?: RequestInit): Promise<Response> {
    const res = await fetch(input, init);
    if (res.status !== 401) return res;

    if (!refreshPromise) {
        refreshPromise = fetch('/api/auth/refresh', {method: 'POST'})
            .then(r => r.ok)
            .finally(() => {
                refreshPromise = null;
            });
    }

    const refreshed = await refreshPromise;
    if (!refreshed) {
        window.dispatchEvent(new Event('auth:logout'));
        return res;
    }
    return fetch(input, init);
}
