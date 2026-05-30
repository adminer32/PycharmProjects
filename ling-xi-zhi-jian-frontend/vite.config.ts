import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
    plugins: [
        vue(),
        Components({
            resolvers: [
                AntDesignVueResolver({
                    importStyle: false, // css in js
                }),
            ],
        }),
        visualizer({
            gzipSize: true,
            brotliSize: true,
            emitFile: false,
            filename: 'rollup-plugin-visualizer.html',
            open: false,
        }),
    ],

    build: {
        rollupOptions: {
            // 更多配置 https://cn.rollupjs.org/configuration-options/
            output: {
                dir: 'ling-xi-zhi-jian-dist', // 指定输出目录为
                format: 'es', // 指定输出的格式，如 es、cjs、amd、iife、umd 等。
                chunkFileNames: 'js/[name]-[hash].js',
                entryFileNames: 'js/[name]-[hash].js',
                assetFileNames: '[ext]/[name]-[hash].[ext]',
                manualChunks(id) {
                    // 手动分割代码
                    if (id.includes('node_modules')) {
                        return 'vendor'; // 将 node_modules 中的代码单独打包成一个名为 vendor 的 JS 文件
                    }
                },
            },
            plugins: [
                // 在这里添加 Rollup 插件
            ],
        },
    },
    resolve: {
        alias: {
            '@': fileURLToPath(new URL('./src', import.meta.url)),
        },
    },
    css: {
        preprocessorOptions: {
            scss: {
                // 向全局sass文件导入变量文件
                additionalData: '@use "@/style/global.scss" as *;',
                api: 'modern', // 或 'modern-compiler'
            },
        },
    },

    server: {
        host: '0.0.0.0',
        port: 5173,
        open: true,
        proxy: {
            '/api': {
                target: 'http://127.0.0.1:8001',
                changeOrigin: true,
                configure: (proxy) => {
                    proxy.on('proxyReq', (proxyReq) => {
                        proxyReq.setHeader('X-Real-IP', '127.0.0.1');
                    });
                },
            },
            '/videos': {
                target: 'http://127.0.0.1:8001',
                changeOrigin: true,
            },
            '/uploads': {
                target: 'http://127.0.0.1:8001',
                changeOrigin: true,
            },
            '/gvhmr': {
                target: 'http://127.0.0.1:8000',
                changeOrigin: true,
                rewrite: (path) => path.replace(/^\/gvhmr/, ''),
            },
        },
    },
});
