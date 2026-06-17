import {computed, reactive} from 'vue';

interface AuthUser {
    id: string;
    name: string;
    email: string;
    emailVerified: boolean;
}

const state = reactive<{ user: AuthUser | null; initializing: boolean }>({
    user: null,
    initializing: true,
});

export async function initAuth() {
    try {
        const res = await fetch('/api/auth/me');
        state.user = res.ok ? await res.json() : null;
    } catch {
        state.user = null;
    } finally {
        state.initializing = false;
    }
    window.addEventListener('auth:logout', () => {
        state.user = null;
    });
}

export function useAuth() {
    async function login(email: string, password: string): Promise<string | null> {
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({email, password}),
            });
            if (res.ok) {
                state.user = await res.json();
                return null;
            }
            const body = await res.json().catch(() => ({}));
            if (res.status === 403 && body.code === 'EMAIL_NOT_VERIFIED') {
                return 'EMAIL_NOT_VERIFIED';
            }
            return 'Invalid email or password.';
        } catch {
            return 'Something went wrong. Please try again.';
        }
    }

    async function signup(
        name: string,
        email: string,
        password: string
    ): Promise<{ pendingVerification: true } | string> {
        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({name, email, password}),
            });
            if (res.status === 201) return {pendingVerification: true};
            const body = await res.json().catch(() => ({}));
            if (res.status === 409) return 'This email is already registered.';
            if (res.status === 400 && body.errors?.password) return body.errors.password;
            return 'Registration failed. Please try again.';
        } catch {
            return 'Something went wrong. Please try again.';
        }
    }

    async function logout(): Promise<void> {
        await fetch('/api/auth/logout', {method: 'POST'}).catch(() => {
        });
        state.user = null;
    }

    async function forgotPassword(email: string): Promise<void> {
        await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({email}),
        }).catch(() => {
        });
    }

    async function resetPassword(token: string, password: string): Promise<string | null> {
        try {
            const res = await fetch('/api/auth/reset-password', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({token, newPassword: password}),
            });
            if (res.ok) return null;
            const body = await res.json().catch(() => ({}));
            if (body.code === 'TOKEN_EXPIRED') return 'This link has expired. Please request a new one.';
            return 'Invalid or already used reset link.';
        } catch {
            return 'Something went wrong. Please try again.';
        }
    }

    async function verifyEmail(token: string): Promise<string | null> {
        try {
            const res = await fetch('/api/auth/verify-email', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({token}),
            });
            if (res.ok) return null;
            const body = await res.json().catch(() => ({}));
            if (body.code === 'TOKEN_EXPIRED') return 'This verification link has expired. Please request a new one.';
            return 'Invalid or already used verification link.';
        } catch {
            return 'Something went wrong. Please try again.';
        }
    }

    async function resendVerification(email: string): Promise<void> {
        await fetch('/api/auth/resend-verification', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({email}),
        }).catch(() => {
        });
    }

    return {
        user: computed(() => state.user),
        isAuthenticated: computed(() => state.user !== null),
        initializing: computed(() => state.initializing),
        login,
        signup,
        logout,
        forgotPassword,
        resetPassword,
        verifyEmail,
        resendVerification,
    };
}
