const breakpoint = {
    watch: ['0px', '319px'],
    phone: ['320px', '480px'],
    pad: ['481px', '768px'],
    notebook: ['769px', '1024px'],
    desktop: ['1025px', '1200px'],
    tv: '1201px', // 16 英寸及以上
} as {
    [device: string]: string | string[];
};

let currentDeviceType = 'tv';

for (const k in breakpoint) {
    if (Array.isArray(breakpoint[k])) {
        // if (window.matchMedia(`only stream and (min-width: ${breakpoint[k][0]}) and (max-width: ${breakpoint[k][1]})`).matches) {
        //     currentDeviceType = k;
        // }   // 这种方式不太管用
        console.log('window.innerWidth', window.innerWidth);
        if (
            window.innerWidth >=
                parseInt(breakpoint[k][0].replace('px', ''), 10) &&
            window.innerWidth <=
                parseInt(breakpoint[k][1].replace('px', ''), 10)
        ) {
            currentDeviceType = k;
        }
    }
}

console.log('设备类型', currentDeviceType);

export default class MediaQueryUtil {
    static match<ReturnType>(data: {
        [device: string]: ReturnType;
    }): ReturnType {
        return data[currentDeviceType];
    }

    static is(
        device: 'watch' | 'phone' | 'pad' | 'notebook' | 'desktop' | 'tv',
    ) {
        return device === currentDeviceType;
    }
}
