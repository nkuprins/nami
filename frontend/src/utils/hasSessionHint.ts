// `has_session` is a lightweight, non-httpOnly cookie the backend sets whenever
// it issues a refresh token (login/refresh). Its presence lets us skip the
// /auth/me + /auth/refresh round trip entirely for visitors who never logged in.
export function hasSessionHint(): boolean {
  return document.cookie.includes('has_session=1');
}
