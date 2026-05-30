/// <reference types="vite/client" />
/// <reference types="ant-design-vue/typings/global" />

// 用于扩展 import.meta 的类型。
interface ImportMeta {
    readonly env: {
        readonly BASE_URL: string;
        readonly VITE_BACK_END_BASE_URL: string;
        readonly VITE_WS_BASE_URL: string;
        readonly VITE_PYTHON_BACK_END_BASE_URL: string;
    };
}

// 扩展 Window 接口
interface Window {
    showNotice: (message: string, type: 'info' | 'warning' | 'success' | 'error', duration?: number) => void;
}
