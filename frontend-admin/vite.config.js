import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from "node:path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8099',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api'),
      },
      '/actuator': {
        target: 'http://localhost:8099',
        changeOrigin: true,
      },
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'), // More robust path resolution

      /*
            '@': '/src', // Setting up @ alias for src directory
      */
    },
  },
});
