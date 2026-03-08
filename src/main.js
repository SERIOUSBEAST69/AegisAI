import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import './assets/theme.css';

// 全局错误处理
window.onerror = function(msg, url, line, col, error) {
  console.error('全局错误:', msg, url, line, col, error);
};

const app = createApp(App);
app.use(router);
app.use(createPinia());
app.use(ElementPlus);
app.mount('#app');
