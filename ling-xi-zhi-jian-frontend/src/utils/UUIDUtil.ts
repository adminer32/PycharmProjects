export default class UUIDUtil {
    static generate() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(
            /[xy]/g,
            function (c) {
                const r = (Math.random() * 16) | 0; // 随机生成一个0到15的整数
                const v = c === 'x' ? r : (r & 0x3) | 0x8; // 如果是'y'，确保高两位为10（即8、9、a、b）
                return v.toString(16); // 转换为16进制字符串
            },
        );
    }
}
