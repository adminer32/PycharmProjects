// eslint.config.js
import { defineConfig } from 'eslint/config';

export default defineConfig([
    {
        ignores: ['**/iconfont.css', 'README.md'],
        rules: {
            semi: 'error',
            'prefer-const': 'error',
        },
    },
]);
