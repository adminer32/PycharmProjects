const keepElOnTop = (el: HTMLElement) => {
    let maxZIndex = 0;
    const parentNode = el.parentNode;
    for (const child of parentNode!.children) {
        const zIndex = window.getComputedStyle(child).zIndex;
        if (zIndex !== 'auto') {
            maxZIndex = Math.max(maxZIndex, parseInt(zIndex));
        }
    }

    el.style.zIndex = (maxZIndex + 1).toString();
};
export default {
    mounted(el: HTMLElement) {
        el.onmousedown = (e) => {
            keepElOnTop(el);
            const x = e.pageX - el.offsetLeft;
            const y = e.pageY - el.offsetTop;
            window.onmousemove = (e) => {
                const cx = e.pageX - x;
                const cy = e.pageY - y;
                el.style.left = `${cx}px`;
                el.style.top = `${cy}px`;
            };
            window.onmouseup = () => {
                window.onmousemove = null;
                window.onmouseup = null;
            };
        };
    },
    unmounted(el: HTMLElement) {
        el.onmousedown = null;
    },
};
