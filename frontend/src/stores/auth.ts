import {computed, readonly, ref} from 'vue';
import {defineStore} from 'pinia';
import {logger} from '../utils/logger';
import {fetchApi} from "../api/fetchApi";


interface AuthUser {
    id: string;
    name: string;
    email: string;
    emailVerified: boolean;
}

export const useAuthStore = defineStore('auth', () => {
    const user = ref<AuthUser | null>(null);
    const initializing = ref(true);
    const isAuthenticated = computed(() => user.value !== null);

    const handleGlobalLogout = () => {
        logger.info('[AuthStore] Global logout event intercepted. Clearing user state.');
        user.value = null;
    };
    window.addEventListener('auth:logout', handleGlobalLogout);

    async function init(): Promise<void> {
        logger.info('[AuthStore] Initializing auth session checking...');
        try {
            const res = await fetchApi(`/api/auth/me`);
            if (res.ok) {
                user.value = await res.json();
                logger.info(`[AuthStore] Session restored successfully for: ${user.value?.email}`);
            } else {
                logger.info('[AuthStore] No active session found (Guest mode).');
                user.value = null;
            }
        } catch (e) {
            logger.error('[AuthStore] Critical failure during session restoration:', e);
            user.value = null;
        } finally {
            initializing.value = false;
        }
    }

    async function login(email: string, password: string): Promise<string | null> {
        logger.info(`[AuthStore] Inbound login attempt for: ${email}`);
        try {
            const res = await fetch(`/api/auth/login`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({email, password}),
            });
            if (res.ok) {
                user.value = await res.json();
                logger.info(`[AuthStore] Login successful. User ID assigned: ${user.value?.id}`);
                return null;
            }
            const body = await res.json().catch(() => ({}));
            if (res.status === 403 && (body as { code?: string }).code === 'EMAIL_NOT_VERIFIED') {
                logger.warn(`[AuthStore] Login rejected: Email unverified for ${email}`);
                return 'EMAIL_NOT_VERIFIED';
            }
            logger.warn(`[AuthStore] Login rejected: Invalid credentials for ${email} (Status: ${res.status})`);
            return 'Invalid email or password.';
        } catch (e) {
            logger.error(`[AuthStore] Connection error during login for ${email}:`, e);
            return 'Something went wrong. Please try again.';
        }
    }

    async function signup(
        name: string,
        email: string,
        password: string,
    ): Promise<{ pendingVerification: true } | string> {
        logger.info(`[AuthStore] Attempting registration for: ${email}`);
        try {
            const res = await fetch(`/api/auth/register`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({name, email, password}),
            });

            if (res.status === 201) {
                logger.info(`[AuthStore] Account created successfully for: ${email}. Awaiting verification.`);
                return {pendingVerification: true};
            }

            const body = await res.json().catch(() => ({})) as { errors?: { password?: string } };

            if (res.status === 409) {
                logger.warn(`[AuthStore] Registration conflict: ${email} is already taken.`);
                return 'This email is already registered.';
            }
            if (res.status === 400 && body.errors?.password) {
                logger.warn(`[AuthStore] Registration validation failed for ${email}: ${body.errors.password}`);
                return body.errors.password;
            }

            logger.warn(`[AuthStore] Registration rejected with code ${res.status} for ${email}`);
            return 'Registration failed. Please try again.';
        } catch (e) {
            logger.error(`[AuthStore] Connection error during signup for ${email}:`, e);
            return 'Something went wrong. Please try again.';
        }
    }

    async function logout(): Promise<void> {
        logger.info('[AuthStore] Instigating explicit user logout sequence.');
        try {
            await fetch(`/api/auth/logout`, {method: 'POST'});
            logger.info('[AuthStore] Server-side session cleared successfully.');
        } catch (e) {
            logger.warn('[AuthStore] Non-fatal endpoint failure during logout:', e);
        } finally {
            user.value = null;
            logger.debug('[AuthStore] Local authentication state flushed clean.');
        }
    }

    async function forgotPassword(email: string): Promise<void> {
        logger.info(`[AuthStore] Requesting password recovery link for: ${email}`);
        try {
            await fetch(`/api/auth/forgot-password`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({email}),
            });
            logger.info(`[AuthStore] Password reset request dispatched for: ${email}`);
        } catch (e) {
            logger.warn(`[AuthStore] Request failed for forgotPassword (${email}):`, e);
        }
    }

    async function resetPassword(token: string, password: string): Promise<string | null> {
        logger.info('[AuthStore] Attempting token-based credential reset.');
        try {
            const res = await fetch(`/api/auth/reset-password`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({token, newPassword: password}),
            });

            if (res.ok) {
                logger.info('[AuthStore] Password reset complete.');
                return null;
            }

            const body = await res.json().catch(() => ({})) as { code?: string };
            if (body.code === 'TOKEN_EXPIRED') {
                logger.warn('[AuthStore] Password reset rejected: Token expired.');
                return 'This link has expired. Please request a new one.';
            }

            logger.warn(`[AuthStore] Password reset rejected: Token invalid or already consumed. (Status: ${res.status})`);
            return 'Invalid or already used reset link.';
        } catch (e) {
            logger.error('[AuthStore] Connection error during password reset:', e);
            return 'Something went wrong. Please try again.';
        }
    }

    async function verifyEmail(token: string): Promise<string | null> {
        logger.info('[AuthStore] Processing email confirmation token token...');
        try {
            const res = await fetch(`/api/auth/verify-email`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({token}),
            });

            if (res.ok) {
                logger.info('[AuthStore] Email successfully verified.');
                return null;
            }

            const body = await res.json().catch(() => ({})) as { code?: string };
            if (body.code === 'TOKEN_EXPIRED') {
                logger.warn('[AuthStore] Email verification rejected: Token expired.');
                return 'This verification link has expired. Please request a new one.';
            }

            logger.warn(`[AuthStore] Email verification rejected: Token invalid. (Status: ${res.status})`);
            return 'Invalid or already used verification link.';
        } catch (e) {
            logger.error('[AuthStore] Connection error during email verification:', e);
            return 'Something went wrong. Please try again.';
        }
    }

    async function resendVerification(email: string): Promise<void> {
        logger.info(`[AuthStore] Requesting verification link duplicate for: ${email}`);
        try {
            await fetch(`/api/auth/resend-verification`, {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({email}),
            });
            logger.info(`[AuthStore] Verification link resent to: ${email}`);
        } catch (e) {
            logger.warn(`[AuthStore] Request failed for resendVerification (${email}):`, e);
        }
    }

    return {
        user: readonly(user),
        isAuthenticated,
        initializing: readonly(initializing),
        init,
        login,
        signup,
        logout,
        forgotPassword,
        resetPassword,
        verifyEmail,
        resendVerification,
    };
});
