import 'animate.css';

export default class AnimateCSSUtil {
    static once(el: HTMLElement, animationName: string) {
        return new Promise((resolve) => {
            el.classList.add(`animate__animated`, animationName);

            el.addEventListener(
                'animationend',
                (event: Event) => {
                    event.stopPropagation(); // 防止冒泡
                    el.classList.remove(`animate__animated`, animationName);
                    resolve('Animation ended');
                },
                {
                    once: true,
                },
            );
        });
    }
}
