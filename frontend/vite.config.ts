import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true
        },
        '/__monitor/gateway': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/gateway/, '')
        },
        '/__monitor/auth': {
          target: 'http://localhost:8081',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/auth/, '')
        },
        '/__monitor/user': {
          target: 'http://localhost:8082',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/user/, '')
        },
        '/__monitor/event': {
          target: 'http://localhost:8083',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/event/, '')
        },
        '/__monitor/resource': {
          target: 'http://localhost:8084',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/resource/, '')
        },
        '/__monitor/booking': {
          target: 'http://localhost:8085',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/booking/, '')
        },
        '/__monitor/notification': {
          target: 'http://localhost:8086',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/notification/, '')
        },
        '/__monitor/workflow': {
          target: 'http://localhost:8087',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/workflow/, '')
        },
        '/__monitor/analytics': {
          target: 'http://localhost:8088',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/__monitor\/analytics/, '')
        }
      }
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
      globals: true,
      css: true,
      include: ['src/**/*.test.ts', 'src/**/*.test.tsx']
    }
  };
});
