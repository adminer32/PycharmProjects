import { createApp } from 'vue';
import _MessageBox from '../components/反馈/_MessageBox/_MessageBox.vue';

export default class MessageUtil {
    private static readonly messageContainerGap = 10;
    private static messageContainerList: HTMLDivElement[] = [];

    static layout() {
        let lastMessageContainer: HTMLDivElement | null = null;
        for (const messageContainer of MessageUtil.messageContainerList) {
            if (lastMessageContainer) {
                messageContainer.style.top =
                    lastMessageContainer.offsetTop +
                    lastMessageContainer.offsetHeight +
                    MessageUtil.messageContainerGap +
                    'px';
            } else {
                messageContainer.style.top =
                    MessageUtil.messageContainerGap + 'px';
            }
            lastMessageContainer = messageContainer;
        }
    }

    private static show(
        str: string,
        type: 'success' | 'warning' | 'error' | 'info',
        duration: number = 1.7,
    ) {
        const div = document.createElement('div');
        div.style.position = 'fixed';
        div.style.left = '50%';
        div.style.transform = 'translateX(-50%)';
        div.style.userSelect = 'none';
        div.style.zIndex = '9999';
        const animation = div.animate(
            [
                { transform: 'translateY(-100%) translateX(-50%)', opacity: 0 },
                { transform: 'translateY(0%) translateX(-50%)', opacity: 1 },
            ],
            {
                duration: 200,
            },
        );
        document.body.append(div);
        MessageUtil.messageContainerList.push(div);
        MessageUtil.layout();
        const app = createApp(_MessageBox, {
            type,
            msg: str,
        });
        app.mount(div);
        animation.play();
        setTimeout(() => {
            const animation = div.animate(
                [
                    { transform: 'translateY(0) translateX(-50%)', opacity: 1 },
                    {
                        transform: 'translateY(-100%) translateX(-50%)',
                        opacity: 0,
                    },
                ],
                {
                    duration: 200,
                },
            );
            animation.play();
            animation.onfinish = () => {
                app.unmount();
                MessageUtil.messageContainerList.splice(
                    MessageUtil.messageContainerList.indexOf(div),
                    1,
                );
                MessageUtil.layout();
                div.remove();
            };
        }, duration * 1000);
    }

    static success(str: string, duration?: number) {
        return MessageUtil.show(str, 'success', duration);
    }

    static error(str: string, duration?: number) {
        return MessageUtil.show(str, 'error', duration);
    }

    static info(str: string, duration?: number) {
        return MessageUtil.show(str, 'info', duration);
    }

    static warning(str: string, duration?: number) {
        return MessageUtil.show(str, 'warning', duration);
    }
}
