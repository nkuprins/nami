import { computed, readonly, ref, onScopeDispose } from 'vue';
import { defineStore } from 'pinia';
import { logger } from '../utils/logger';
import { hasSessionHint } from '../utils/hasSessionHint';
import {
  authApi,
  type AuthUser,
  ERROR_EMAIL_NOT_VERIFIED,
} from '../api/authApi';

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null);
  const initializing = ref(true);
  const isAuthenticated = computed(() => user.value !== null);

  const handleGlobalLogout = () => {
    logger.info(
      '[AuthStore] Global logout event intercepted. Clearing user state.'
    );
    user.value = null;
  };

  window.addEventListener('auth:logout', handleGlobalLogout);

  onScopeDispose(() => {
    window.removeEventListener('auth:logout', handleGlobalLogout);
  });

  let initPromise: Promise<void> | null = null;

  // Idempotent: the app boot and the router guard both call this, but the
  // session should only be fetched once.
  function init(): Promise<void> {
    initPromise ??= runInit();
    return initPromise;
  }

  async function runInit(): Promise<void> {
    logger.info('[AuthStore] Initializing auth session checking...');
    if (!hasSessionHint()) {
      logger.info(
        '[AuthStore] No session hint cookie found. Skipping getMe() for anonymous visitor.'
      );
      user.value = null;
      initializing.value = false;
      return;
    }
    try {
      const userData = await authApi.getMe();
      if (userData) {
        user.value = userData;
        logger.info(
          `[AuthStore] Session restored successfully for: ${user.value?.email}`
        );
      } else {
        logger.info('[AuthStore] No active session found (Guest mode).');
        user.value = null;
      }
    } catch (e) {
      logger.error(
        '[AuthStore] Critical failure during session restoration:',
        e
      );
      user.value = null;
    } finally {
      initializing.value = false;
    }
  }

  async function login(
    email: string,
    password: string
  ): Promise<string | null> {
    logger.info(`[AuthStore] Inbound login attempt for: ${email}`);
    const { user: loggedInUser, error } = await authApi.login(email, password);

    if (error) {
      if (error === ERROR_EMAIL_NOT_VERIFIED) {
        logger.warn(
          `[AuthStore] Login rejected: Email unverified for ${email}`
        );
        return ERROR_EMAIL_NOT_VERIFIED;
      }
      logger.warn(
        `[AuthStore] Login rejected: Invalid credentials for ${email}`
      );
      return error;
    }

    if (loggedInUser) {
      user.value = loggedInUser;
      logger.info(
        `[AuthStore] Login successful. User ID assigned: ${user.value?.id}`
      );
      return null;
    }

    return 'Something went wrong. Please try again.';
  }

  async function signup(
    name: string,
    email: string,
    password: string
  ): Promise<{ pendingVerification: true } | string> {
    logger.info(`[AuthStore] Attempting registration for: ${email}`);
    const result = await authApi.signup(name, email, password);

    if (typeof result === 'object' && result.pendingVerification) {
      logger.info(
        `[AuthStore] Account created successfully for: ${email}. Awaiting verification.`
      );
    } else {
      logger.warn(
        `[AuthStore] Registration validation or conflict failed for ${email}`
      );
    }
    return result;
  }

  async function logout(): Promise<void> {
    logger.info('[AuthStore] Instigating explicit user logout sequence.');
    await authApi.logout();
    user.value = null;
    logger.debug('[AuthStore] Local authentication state flushed clean.');
  }

  async function forgotPassword(email: string): Promise<void> {
    logger.info(`[AuthStore] Requesting password recovery link for: ${email}`);
    await authApi.forgotPassword(email);
    logger.info(`[AuthStore] Password reset request dispatched for: ${email}`);
  }

  async function resetPassword(
    token: string,
    password: string
  ): Promise<string | null> {
    logger.info('[AuthStore] Attempting token-based credential reset.');
    const error = await authApi.resetPassword(token, password);
    if (!error) {
      logger.info('[AuthStore] Password reset complete.');
      return null;
    }
    logger.warn(`[AuthStore] Password reset rejected: ${error}`);
    return error;
  }

  async function verifyEmail(token: string): Promise<string | null> {
    logger.info('[AuthStore] Processing email confirmation token token...');
    const error = await authApi.verifyEmail(token);
    if (!error) {
      logger.info('[AuthStore] Email successfully verified.');
      return null;
    }
    logger.warn(`[AuthStore] Email verification rejected: ${error}`);
    return error;
  }

  async function resendVerification(email: string): Promise<void> {
    logger.info(
      `[AuthStore] Requesting verification link duplicate for: ${email}`
    );
    await authApi.resendVerification(email);
    logger.info(`[AuthStore] Verification link resent to: ${email}`);
  }

  async function updateProfile(
    name: string,
    email: string
  ): Promise<{ error: string | null; emailChanged: boolean }> {
    logger.info('[AuthStore] Updating profile...');
    const oldEmail = user.value?.email ?? '';
    const { user: updated, error } = await authApi.updateProfile({
      name: name.trim() || undefined,
      email: email.trim() || undefined,
    });
    if (updated) {
      const emailChanged =
        updated.email.toLowerCase() !== oldEmail.toLowerCase();
      user.value = updated;
      logger.info('[AuthStore] Profile updated.');
      return { error: null, emailChanged };
    }
    return { error, emailChanged: false };
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
    updateProfile,
  };
});
