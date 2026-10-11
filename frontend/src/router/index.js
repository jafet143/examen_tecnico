import { createRouter, createWebHistory } from 'vue-router'
import ResumenVista from '../vistas/ResumenVista.vue'
import ListaClientesVista from '../vistas/ListaClientesVista.vue'
import FormularioClienteVista from '../vistas/FormularioClienteVista.vue'
import DetalleClienteVista from '../vistas/DetalleClienteVista.vue'
import ListaSolicitudesVista from '../vistas/ListaSolicitudesVista.vue'
import NuevaSolicitudVista from '../vistas/NuevaSolicitudVista.vue'
import RevisionSolicitudVista from '../vistas/RevisionSolicitudVista.vue'
import ListaCuentasVista from '../vistas/ListaCuentasVista.vue'
import DetalleCuentaVista from '../vistas/DetalleCuentaVista.vue'
import NoEncontradoVista from '../vistas/NoEncontradoVista.vue'
import SinAccesoVista from '../vistas/SinAccesoVista.vue'
import { obtenerTarjeta } from '../servicios/tarjetasServicio'
import { tieneAlgunPermiso } from '../autenticacion/servicioAutenticacion'
import { PERMISOS } from '../utilidades/permisos'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: (destino, origen, posicionGuardada) => posicionGuardada ?? { top: 0 },
  routes: [
    { path: '/', redirect: '/resumen' },
    { path: '/resumen', name: 'resumen', component: ResumenVista },
    { path: '/clientes', name: 'lista-clientes', component: ListaClientesVista, meta: { permisos: [PERMISOS.CLIENTES_CONSULTAR] } },
    { path: '/clientes/nuevo', name: 'nuevo-cliente', component: FormularioClienteVista, meta: { permisos: [PERMISOS.CLIENTES_GESTIONAR] } },
    { path: '/clientes/:idCliente/editar', name: 'editar-cliente', component: FormularioClienteVista, meta: { permisos: [PERMISOS.CLIENTES_GESTIONAR] } },
    { path: '/clientes/:idCliente', name: 'detalle-cliente', component: DetalleClienteVista, meta: { permisos: [PERMISOS.CLIENTES_CONSULTAR] } },
    { path: '/solicitudes', name: 'lista-solicitudes', component: ListaSolicitudesVista, meta: { permisos: [PERMISOS.SOLICITUDES_CONSULTAR] } },
    { path: '/solicitudes/nueva', name: 'nueva-solicitud', component: NuevaSolicitudVista, meta: { permisos: [PERMISOS.SOLICITUDES_REGISTRAR] } },
    { path: '/solicitudes/:idSolicitud/revision', name: 'revision-solicitud', component: RevisionSolicitudVista, meta: { permisos: [PERMISOS.SOLICITUDES_CONSULTAR] } },
    { path: '/cuentas', name: 'lista-cuentas', component: ListaCuentasVista, meta: { permisos: [PERMISOS.CUENTAS_CONSULTAR] } },
    { path: '/cuentas/:idCuenta', name: 'detalle-cuenta', component: DetalleCuentaVista, meta: { permisos: [PERMISOS.CUENTAS_CONSULTAR] } },
    { path: '/tarjetas', redirect: '/cuentas' },
    {
      path: '/tarjetas/:idTarjeta',
      component: NoEncontradoVista,
      meta: { permisos: [PERMISOS.CUENTAS_CONSULTAR] },
      beforeEnter: async (destino) => {
        try {
          const tarjeta = await obtenerTarjeta(destino.params.idTarjeta)
          return `/cuentas/${tarjeta.idCuenta}`
        } catch {
          return '/cuentas'
        }
      }
    },
    { path: '/sin-acceso', name: 'sin-acceso', component: SinAccesoVista },
    { path: '/:pathMatch(.*)*', name: 'no-encontrado', component: NoEncontradoVista }
  ]
})

router.beforeEach((destino) => {
  const permisosRequeridos = destino.meta.permisos
  if (permisosRequeridos && !tieneAlgunPermiso(permisosRequeridos)) return { name: 'sin-acceso', replace: true }
})

export default router
