import {defineConfig} from 'vite';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite'; // 1. Import the new plugin

export default defineConfig({
    plugins: [
        vue(),
        tailwindcss(), // 2. Add it to your execution pipeline
    ],
});
