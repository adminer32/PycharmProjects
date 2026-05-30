const map = new WeakMap();

const ob = new IntersectionObserver((entries) => {
    for (const entry of entries) {
        // 该元素和视口香蕉播放该元素的动画
        if (entry.isIntersecting) {
            map.get(entry.target).play();
            ob.unobserve(entry.target);
        }
    }
});

const isBelowViewport = (el: HTMLElement) => {
    return el.getBoundingClientRect().top > window.innerHeight;
};

export default {
    mounted(el: HTMLElement) {
        if (!isBelowViewport(el)) {
            return;
        }
        const animation = el.animate(
            [
                {
                    opacity: 0.5,
                    transform: 'translateY(100px)',
                },
                {
                    opacity: 1,
                    transform: 'translateY(0)',
                },
            ],
            {
                duration: 300,
                easing: 'ease-out',
                // fill: "forwards" // 动画结束后保持最后一个关键帧的状态
            },
        );
        animation.pause(); // 初始状态下暂停动画
        ob.observe(el);
        map.set(el, animation); // 将动画效果保存到 WeakMap 中
    },
    unmounted(el: HTMLElement) {
        // // 删除动画
        // const animation = map.get(el);
        // animation.cancel();

        //  清除观察器
        ob.unobserve(el);
    },
};
