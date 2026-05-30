import { createApp } from 'vue';
import { createPinia } from 'pinia';
import vSlideIn from '@/directives/vSlideIn';
import vDrag from '@/directives/vDrag';
import vSizeOb from '@/directives/vSizeOb';
import { i18n } from '@/i18n';
import App from './App.vue';
import router from './router';

const app = createApp(App);
app.directive('drag', vDrag);
app.directive('slide-in', vSlideIn);
app.directive('size-ob', vSizeOb);

app.use(createPinia());
app.use(i18n);
app.use(router);
app.mount('#app');
