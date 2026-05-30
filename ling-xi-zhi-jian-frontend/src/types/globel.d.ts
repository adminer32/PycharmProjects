export type Result<T = unknown> = {
    status: 'success' | 'error';
    message: string;
    data: T;
};

export enum ThemeEnum {
    AUTO = 'auto', // 跟随系统
    LIGHT = 'light',
    DARK = 'dark',
}

declare module '*.json' {
    const value: Record<string, unknown>;
    export default value;
}
