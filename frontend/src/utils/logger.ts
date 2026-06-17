const isDev = import.meta.env.DEV;

export const logger = {
    info: (...a: unknown[]) => {
        if (isDev) console.info(...a);
    },
    warn: (...a: unknown[]) => {
        if (isDev) console.warn(...a);
    },
    error: (...a: unknown[]) => console.error(...a),
    debug: (...a: unknown[]) => {
        if (isDev) console.log(...a);
    },
};
