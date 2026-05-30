export default class LocalStorageUtil {
    static get<T>(
        key: string,
        defaultValue: T,
        isValid?: (str: string) => boolean,
    ): T {
        const item = localStorage.getItem(key);
        if (item) {
            // 有值
            if (isValid) {
                // 合法
                if (isValid(item)) {
                    return item as T; // 目标
                } else {
                    return defaultValue;
                }
            } else {
                return item as T; // 默认
            }
        }
        return defaultValue; // 默认
    }

    static set(key: string, value: string) {
        localStorage.setItem(key, value);
    }

    static remove(key: string) {
        localStorage.removeItem(key);
    }
}
