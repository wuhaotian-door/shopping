import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import router from './router'
import './style.css'
import App from './App.vue'

const app = createApp(App)

app.config.errorHandler = (err, _instance, info) => {
  console.error('Global Vue error:', err, info)
  // Show error on page so user can see it
  const el = document.getElementById('app')
  if (el) {
    el.innerHTML = '<div style="padding:40px;color:red;font-family:monospace;white-space:pre-wrap;background:#fff;"><h2>Vue Error</h2>' +
      (err instanceof Error ? (err.message + '<br><br>' + (err.stack || '')) : String(err)) +
      '<br><br><b>Info:</b> ' + info + '</div>'
  }
}

app.use(createPinia())
app.use(ElementPlus, { locale: zhCn })
app.use(router)

app.mount('#app')
