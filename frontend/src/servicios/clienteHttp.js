import axios from 'axios'
import { obtenerTokenActualizado, iniciarSesion } from '../autenticacion/servicioAutenticacion'

const clienteHttp = axios.create({
  baseURL: import.meta.env.VITE_URL_API,
  headers: { 'Content-Type': 'application/json' }
})

clienteHttp.interceptors.request.use(async (configuracion) => {
  configuracion.headers.Authorization = 'Bearer ' + (await obtenerTokenActualizado())
  return configuracion
})

clienteHttp.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    if (error.response?.status === 401) {
      iniciarSesion()
    }
    return Promise.reject(error)
  }
)

export default clienteHttp
