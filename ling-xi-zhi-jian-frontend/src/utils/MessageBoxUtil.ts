import { createApp } from 'vue';
import Modal from '@/components/反馈/Modal/Modal.vue';

export default class MessageBoxUtil {
    static confirm(tips: string) {
        return new Promise<boolean>((resolve) => {
            const div = document.createElement('div');
            document.body.append(div);
            const app = createApp(Modal, {
                open: true,
                title: tips,
                ok: () => {
                    resolve(true);
                },
                cancel: () => {
                    resolve(false);
                },
            });
            app.mount(div);
        });
    }
}
