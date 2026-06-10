import {computed, reactive} from 'vue';

const STORAGE_KEY = 'nami:user';

interface AuthUser {
    name: string;
    email: string;
}

interface AuthState {
    user: AuthUser | null;
}

function load(): AuthUser | null {
    try {
        const raw = localStorage.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

const state = reactive<AuthState>({user: load()});

export function useAuth() {
    const isAuthenticated = computed(() => state.user !== null);
    const user = computed(() => state.user);

    function login(email: string, password: string): boolean {
        if (!email || !password) return false;
        const mockUser: AuthUser = {name: email.split('@')[0], email};
        state.user = mockUser;
        localStorage.setItem(STORAGE_KEY, JSON.stringify(mockUser));
        return true;
    }

    function signup(name: string, email: string, password: string): boolean {
        if (!name || !email || !password) return false;
        const mockUser: AuthUser = {name, email};
        state.user = mockUser;
        localStorage.setItem(STORAGE_KEY, JSON.stringify(mockUser));
        return true;
    }

    function logout() {
        state.user = null;
        localStorage.removeItem(STORAGE_KEY);
    }

    return {isAuthenticated, user, login, signup, logout};
}
