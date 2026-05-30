const map = new WeakMap<HTMLElement, (width: number, height: number) => void>();

const ob = new ResizeObserver((entries: ResizeObserverEntry[]) => {
    for (const entry of entries) {
        const handler = map.get(entry.target as HTMLElement);
        if (handler) {
            const box = entry.contentRect; // 注意：这里通常使用contentRect而不是borderBoxSize
            handler(box.width, box.height); // 调用回调函数 (一旦被观察就会调用一次)
        }
    }
});

interface Binding {
    value(width: number, height: number): void;
}

export default {
    mounted: (el: HTMLElement, binding: Binding) => {
        // binding 是一个对象
        ob.observe(el);
        map.set(el, binding.value);
    },
    unmounted: (el: HTMLElement) => {
        ob.unobserve(el);
    },
};
