import {setupWorker} from 'msw/browser';
import {handlers} from './handlers';

export const worker = setupWorker(...handlers);

export function installMock(): Promise<ServiceWorkerRegistration | undefined> {
    console.info('[mock] MSW Service Worker installing...');
    // worker.start() returns a promise, ensuring initialization completes safely
    return worker.start({
        onUnhandledRequest: 'bypass', // Lets normal non-mocked requests slip through to real servers
    });
}

// import {mockListings} from './listings';
// import {DISTRICTS} from '../data/locations';
//
// const PAGE_SIZE = 12;
//
// const nameToSlug = new Map(DISTRICTS.map(d => [d.name, d.slug]));
//
// // Convert human-readable district names to slugs to match backend DTO format
// const dtoCatalog = mockListings.map(item => ({
//     ...item,
//     district: nameToSlug.get(item.district) ?? item.district.toLowerCase(),
// }));
//
// function jsonRes(body: unknown, status = 200): Response {
//     return new Response(JSON.stringify(body), {
//         status,
//         headers: {'Content-Type': 'application/json'},
//     });
// }
//
// function emptyRes(status: number): Response {
//     return new Response(null, {status});
// }
//
// let mockUser: {id: string; name: string; email: string; emailVerified: boolean} | null = null;
//
// function parseBody(init?: RequestInit): Record<string, unknown> {
//     try {
//         return JSON.parse((init?.body as string) ?? '{}');
//     } catch {
//         return {};
//     }
// }
//
// function handleAuth(path: string, method: string, init?: RequestInit): Response | null {
//     if (path === '/api/auth/me' && method === 'GET') {
//         return mockUser ? jsonRes(mockUser) : emptyRes(401);
//     }
//     if (path === '/api/auth/login' && method === 'POST') {
//         const {email, password} = parseBody(init) as {email: string; password: string};
//         if (password?.length >= 1) {
//             mockUser = {id: 'mock-user-1', name: email.split('@')[0], email, emailVerified: true};
//             return jsonRes(mockUser);
//         }
//         return jsonRes({message: 'Invalid credentials'}, 401);
//     }
//     if (path === '/api/auth/register' && method === 'POST') {
//         return emptyRes(201);
//     }
//     if (path === '/api/auth/logout' && method === 'POST') {
//         mockUser = null;
//         return emptyRes(200);
//     }
//     if (path === '/api/auth/refresh' && method === 'POST') {
//         return emptyRes(401);
//     }
//     if (
//         (path === '/api/auth/forgot-password' ||
//             path === '/api/auth/reset-password' ||
//             path === '/api/auth/verify-email' ||
//             path === '/api/auth/resend-verification') &&
//         method === 'POST'
//     ) {
//         return emptyRes(200);
//     }
//     return null;
// }
//
// type SortKey = 'newest' | 'price-asc' | 'price-desc' | 'price-per-m2-asc' | 'm2-desc';
//
// function applyFilters(params: URLSearchParams): {items: typeof dtoCatalog; total: number} {
//     let items = [...dtoCatalog];
//
//     const type = params.get('type');
//     if (type) items = items.filter(i => i.type === type);
//
//     const locs = params.getAll('loc');
//     if (locs.length) items = items.filter(i => locs.includes(i.district));
//
//     const priceMin = params.get('priceMin');
//     const priceMax = params.get('priceMax');
//     if (priceMin) items = items.filter(i => i.price >= Number(priceMin));
//     if (priceMax) items = items.filter(i => i.price <= Number(priceMax));
//
//     const rooms = params.getAll('rooms').map(Number);
//     if (rooms.length) items = items.filter(i => rooms.includes(i.rooms));
//
//     const m2Min = params.get('m2Min');
//     const m2Max = params.get('m2Max');
//     if (m2Min) items = items.filter(i => i.m2 >= Number(m2Min));
//     if (m2Max) items = items.filter(i => i.m2 <= Number(m2Max));
//
//     const floorMin = params.get('floorMin');
//     const floorMax = params.get('floorMax');
//     if (floorMin) items = items.filter(i => i.floor != null && i.floor >= Number(floorMin));
//     if (floorMax) items = items.filter(i => i.floor != null && i.floor <= Number(floorMax));
//
//     if (params.get('notGround') === 'true') items = items.filter(i => i.floor == null || i.floor > 1);
//     if (params.get('notTop') === 'true')
//         items = items.filter(i => i.floor == null || i.totalFloors == null || i.floor < i.totalFloors);
//
//     const yearMin = params.get('yearMin');
//     const yearMax = params.get('yearMax');
//     if (yearMin) items = items.filter(i => i.yearBuilt != null && i.yearBuilt >= Number(yearMin));
//     if (yearMax) items = items.filter(i => i.yearBuilt != null && i.yearBuilt <= Number(yearMax));
//
//     const features = params.getAll('features');
//     if (features.length) items = items.filter(i => features.every(f => (i.features as string[]).includes(f)));
//
//     const completion = params.get('completion');
//     if (completion) items = items.filter(i => i.completion === completion);
//
//     const sort = (params.get('sort') ?? 'newest') as SortKey;
//     const sorters: Record<SortKey, (a: (typeof dtoCatalog)[0], b: (typeof dtoCatalog)[0]) => number> = {
//         newest: (a, b) => b.postedAt.localeCompare(a.postedAt),
//         'price-asc': (a, b) => a.price - b.price,
//         'price-desc': (a, b) => b.price - a.price,
//         'price-per-m2-asc': (a, b) => a.price / a.m2 - b.price / b.m2,
//         'm2-desc': (a, b) => b.m2 - a.m2,
//     };
//     items.sort(sorters[sort] ?? sorters.newest);
//
//     const total = items.length;
//     const page = Number(params.get('page') ?? '1');
//     return {items: items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE), total};
// }
//
// export function installMock(): void {
//     const realFetch = globalThis.fetch.bind(globalThis);
//
//     globalThis.fetch = function mockFetch(input, init) {
//         const rawUrl =
//             typeof input === 'string'
//                 ? input
//                 : input instanceof URL
//                   ? input.href
//                   : (input as Request).url;
//
//         if (!rawUrl.startsWith('/api/') && !rawUrl.startsWith('/mock-upload/')) {
//             return realFetch(input, init);
//         }
//
//         const reqInit =
//             input instanceof Request
//                 ? {method: input.method, body: init?.body}
//                 : init;
//         const method = reqInit?.method?.toUpperCase() ?? 'GET';
//         const parsed = new URL(rawUrl, location.origin);
//         const path = parsed.pathname;
//
//         const authRes = handleAuth(path, method, reqInit);
//         if (authRes) return Promise.resolve(authRes);
//
//         if (path === '/api/properties' && method === 'GET') {
//             return Promise.resolve(jsonRes(applyFilters(parsed.searchParams)));
//         }
//         if (path === '/api/properties/mine' && method === 'GET') {
//             return Promise.resolve(jsonRes(mockUser ? dtoCatalog.slice(0, 2) : []));
//         }
//         const singleMatch = path.match(/^\/api\/properties\/([^/]+)$/);
//         if (singleMatch && method === 'GET') {
//             const item = dtoCatalog.find(i => i.id === singleMatch[1]);
//             return Promise.resolve(item ? jsonRes(item) : emptyRes(404));
//         }
//         if (path === '/api/properties' && method === 'POST') {
//             const body = parseBody(reqInit);
//             return Promise.resolve(
//                 jsonRes({...body, id: `mock-${Date.now()}`, postedAt: new Date().toISOString()}, 201),
//             );
//         }
//         if (path === '/api/uploads/presign' && method === 'POST') {
//             const {filenames = []} = parseBody(reqInit) as {filenames: string[]};
//             return Promise.resolve(
//                 jsonRes(
//                     filenames.map((_name, i) => ({
//                         uploadUrl: `/mock-upload/${i}`,
//                         fileUrl: `https://picsum.photos/seed/upload-${Date.now()}-${i}/1200/900`,
//                     })),
//                 ),
//             );
//         }
//         if (path.startsWith('/mock-upload/')) {
//             return Promise.resolve(emptyRes(200));
//         }
//
//         return Promise.resolve(emptyRes(404));
//     } as typeof fetch;
//
//     console.info('[mock] fetch interceptor installed');
// }
