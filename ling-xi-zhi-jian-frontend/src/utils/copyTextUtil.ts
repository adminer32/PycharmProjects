/**
 * 复制文本到剪贴板
 * @param text 要复制的文本
 * @returns 是否复制成功
 */
export function copyTextToClipboard(text: string): boolean {
    // 首先尝试使用现代浏览器的剪贴板API
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard
            .writeText(text)
            .then(() => {
                console.log('Text copied to clipboard using modern API.');
            })
            .catch((error) => {
                console.error('Failed to copy text using modern API:', error);
            });
        return true;
    }

    // 如果现代API不可用，尝试使用传统的选中文本 + 执行命令的方式
    try {
        // 创建一个隐藏的textarea元素
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed'; // 防止滚动到页面顶部
        textarea.style.opacity = '0'; // 隐藏元素
        document.body.appendChild(textarea);

        // 选中textarea中的文本
        textarea.focus();
        textarea.select();

        // 执行复制命令
        const successful = document.execCommand('copy');
        if (successful) {
            console.log('Text copied to clipboard using execCommand.');
        } else {
            console.error('Copy command failed.');
        }

        // 移除临时元素
        document.body.removeChild(textarea);
        return successful;
    } catch (error) {
        console.error('Failed to copy text using execCommand:', error);
        return false;
    }
}
