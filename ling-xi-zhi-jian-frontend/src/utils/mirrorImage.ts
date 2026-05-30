export function mirrorImage(imageUrl: string): Promise<string> {
    return new Promise((resolve, reject) => {
        const img = new Image();
        img.crossOrigin = 'Anonymous'; // 允许跨域请求
        img.src = imageUrl;

        img.onload = () => {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');

            if (!ctx) {
                reject(new Error('Could not get canvas context'));
                return;
            }

            // 设置canvas大小与图片相同
            canvas.width = img.width;
            canvas.height = img.height;

            // 绘制镜像图片
            ctx.translate(canvas.width, 0);
            ctx.scale(-1, 1);
            ctx.drawImage(img, 0, 0);

            // 将canvas转换为Data URL
            const mirroredImageUrl = canvas.toDataURL();
            resolve(mirroredImageUrl);
        };

        img.onerror = (error) => {
            reject(error);
        };
    });
}
