import {createApp} from 'vue';
import App from './App.vue';
import {router} from './router'
import './style.css';

async function main() {
    if (import.meta.env.VITE_MOCK === 'true') {
        const {installMock} = await import('./mock/index');
        await installMock().catch((err) => {
            console.warn("[mock] MSW failed to launch. Network requests will not be mocked:", err);
        });
    }
    createApp(App).use(router).mount('#app');
}

main().catch((error) => {
    console.error("Failed to initialize the application:", error);
});