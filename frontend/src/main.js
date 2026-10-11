import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { inicializarAutenticacion } from './autenticacion/servicioAutenticacion'
import './estilos/principal.css'
import './estilos/animaciones.css'

await inicializarAutenticacion()

createApp(App).use(router).mount('#app')
