import { fetchApi } from './fetchApi';
import { logger } from '../utils/logger';

export const MIN_PASSWORD_LENGTH = 15;
export const ERROR_EMAIL_NOT_VERIFIED = 'EMAIL_NOT_VERIFIED' as const;
const ERROR_TOKEN_EXPIRED = 'TOKEN_EXPIRED';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  emailVerified: boolean;
  admin: boolean;
}

export const authApi = {
  async getMe(): Promise<AuthUser | null> {
    const res = await fetchApi(`/api/auth/me`);
    if (res.ok) {
      return res.json();
    }
    return null;
  },

  async login(
    email: string,
    password: string
  ): Promise<{ user: AuthUser | null; error: string | null }> {
    try {
      const res = await fetchApi(`/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });
      if (res.ok) {
        const user = await res.json();
        return { user, error: null };
      }
      const body = await res.json().catch(() => ({}));
      if (
        res.status === 403 &&
        (body as { code?: string }).code === ERROR_EMAIL_NOT_VERIFIED
      ) {
        return { user: null, error: ERROR_EMAIL_NOT_VERIFIED };
      }
      return { user: null, error: 'Invalid email or password.' };
    } catch (e) {
      logger.error(`[authApi] Connection error during login for ${email}:`, e);
      return {
        user: null,
        error: 'Something went wrong. Please try again.',
      };
    }
  },

  async signup(
    name: string,
    email: string,
    password: string,
    turnstileToken?: string
  ): Promise<{ pendingVerification: true } | string> {
    try {
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      if (turnstileToken) headers['X-Turnstile-Token'] = turnstileToken;
      const res = await fetchApi(`/api/auth/register`, {
        method: 'POST',
        headers,
        body: JSON.stringify({ name, email, password }),
      });

      if (res.status === 201) {
        return { pendingVerification: true };
      }

      const body = (await res.json().catch(() => ({}))) as {
        errors?: { password?: string };
      };

      if (res.status === 409) {
        return 'This email is already registered.';
      }
      if (res.status === 400 && body.errors?.password) {
        return body.errors.password;
      }

      return 'Registration failed. Please try again.';
    } catch (e) {
      logger.error(`[authApi] Connection error during signup for ${email}:`, e);
      return 'Something went wrong. Please try again.';
    }
  },

  async googleSignIn(
    credential: string
  ): Promise<{ user: AuthUser | null; error: string | null }> {
    try {
      const res = await fetchApi(`/api/auth/google`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ credential }),
      });
      if (res.ok) {
        return { user: await res.json(), error: null };
      }
      return { user: null, error: 'Google sign-in failed. Please try again.' };
    } catch (e) {
      logger.error('[authApi] Connection error during Google sign-in:', e);
      return { user: null, error: 'Something went wrong. Please try again.' };
    }
  },

  async updateProfile(payload: {
    name?: string;
    email?: string;
  }): Promise<{ user: AuthUser | null; error: string | null }> {
    try {
      const res = await fetchApi('/api/auth/me', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (res.ok) return { user: await res.json(), error: null };
      const body = (await res.json().catch(() => ({}))) as { code?: string };
      if (res.status === 409 || body.code === 'EMAIL_TAKEN')
        return { user: null, error: 'EMAIL_TAKEN' };
      if (res.status === 400) return { user: null, error: 'NO_CHANGES' };
      return { user: null, error: 'UNKNOWN' };
    } catch (e) {
      logger.error('[authApi] updateProfile failed:', e);
      return { user: null, error: 'UNKNOWN' };
    }
  },

  async exportData(): Promise<object | null> {
    try {
      const res = await fetchApi('/api/auth/export');
      if (res.ok) return res.json();
      return null;
    } catch (e) {
      logger.error('[authApi] exportData failed:', e);
      return null;
    }
  },

  async deleteAccount(): Promise<void> {
    const res = await fetchApi('/api/auth/me', { method: 'DELETE' });
    if (!res.ok) throw new Error(`deleteAccount: ${res.status}`);
  },

  async logout(): Promise<void> {
    try {
      await fetchApi(`/api/auth/logout`, { method: 'POST' });
    } catch (e) {
      logger.warn('[authApi] Non-fatal endpoint failure during logout:', e);
    }
  },

  async forgotPassword(email: string): Promise<void> {
    try {
      await fetchApi(`/api/auth/forgot-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
    } catch (e) {
      logger.warn(`[authApi] Request failed for forgotPassword (${email}):`, e);
    }
  },

  async resetPassword(token: string, password: string): Promise<string | null> {
    try {
      const res = await fetchApi(`/api/auth/reset-password`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token, newPassword: password }),
      });

      if (res.ok) {
        return null;
      }

      const body = (await res.json().catch(() => ({}))) as { code?: string };
      if (body.code === ERROR_TOKEN_EXPIRED) {
        return 'This link has expired. Please request a new one.';
      }

      return 'Invalid or already used reset link.';
    } catch (e) {
      logger.error('[authApi] Connection error during password reset:', e);
      return 'Something went wrong. Please try again.';
    }
  },

  async verifyEmail(token: string): Promise<string | null> {
    try {
      const res = await fetchApi(`/api/auth/verify-email`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
      });

      if (res.ok) {
        return null;
      }

      const body = (await res.json().catch(() => ({}))) as { code?: string };
      if (body.code === ERROR_TOKEN_EXPIRED) {
        return 'This verification link has expired. Please request a new one.';
      }

      return 'Invalid or already used verification link.';
    } catch (e) {
      logger.error('[authApi] Connection error during email verification:', e);
      return 'Something went wrong. Please try again.';
    }
  },

  async resendVerification(email: string): Promise<void> {
    try {
      await fetchApi(`/api/auth/resend-verification`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email }),
      });
    } catch (e) {
      logger.warn(
        `[authApi] Request failed for resendVerification (${email}):`,
        e
      );
    }
  },
};
