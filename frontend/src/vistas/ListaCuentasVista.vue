<script setup>
import { ref, computed, onMounted } from 'vue'
import SelectorPestanas from '../componentes/SelectorPestanas.vue'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import TarjetaIndicador from '../componentes/TarjetaIndicador.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import Paginacion from '../componentes/Paginacion.vue'
import InsigniasEstadoCuenta from '../componentes/InsigniasEstadoCuenta.vue'
import IndicadorTipoTarjeta from '../componentes/IndicadorTipoTarjeta.vue'
import NumeroProtegido from '../componentes/NumeroProtegido.vue'
import { listarCuentas } from '../servicios/cuentasServicio'
import { usarPaginacion } from '../composables/usarPaginacion'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { ESTADOS_PAGO, ESTADO_TARJETA_BLOQUEADA, ESTADO_CUENTA_ACTIVA, ESTADO_CUENTA_CANCELADA } from '../utilidades/constantes'
import { formatearMoneda, formatearPorcentaje, obtenerUltimosCuatroDigitos } from '../utilidades/formatos'
import { obtenerMensajeError } from '../utilidades/errores'

const { tiposTarjeta, cargarCatalogo } = usarCatalogoTiposTarjeta()

const cuentas = ref([])
const textoBusqueda = ref('')
const tipoFiltro = ref('')
const ambitoFiltro = ref(ESTADO_CUENTA_ACTIVA)
const opcionesAmbito = computed(() => [
  { valor: ESTADO_CUENTA_ACTIVA, etiqueta: 'Activas', cantidad: cuentasActivas.value.length },
  { valor: ESTADO_CUENTA_CANCELADA, etiqueta: 'Canceladas', cantidad: cantidadCanceladas.value },
  { valor: '', etiqueta: 'Todas', cantidad: cuentas.value.length }
])
const estadoFiltro = ref('')
const cargando = ref(true)
const mensajeError = ref('')

const tarjetaVigenteBloqueada = (cuenta) => cuenta.tarjetaVigente?.estadoTarjeta === ESTADO_TARJETA_BLOQUEADA
const esCancelada = (cuenta) => cuenta.estadoCuenta === ESTADO_CUENTA_CANCELADA
const cuentasActivas = computed(() => cuentas.value.filter((cuenta) => !esCancelada(cuenta)))
const cantidadCanceladas = computed(() => cuentas.value.length - cuentasActivas.value.length)
const cuentasEnMora = computed(() => cuentasActivas.value.filter((cuenta) => cuenta.estadoPago === 'EN_MORA').length)
const cuentasBloqueadas = computed(() => cuentasActivas.value.filter(tarjetaVigenteBloqueada).length)
const sumar = (campo) => cuentasActivas.value.reduce((total, cuenta) => total + Number(cuenta[campo]), 0)
const limiteTotal = computed(() => sumar('limiteCredito'))
const deudaTotal = computed(() => sumar('saldoActual'))
const disponibleTotal = computed(() => sumar('creditoDisponible'))
const porcentajeUtilizado = computed(() => (limiteTotal.value > 0 ? (deudaTotal.value / limiteTotal.value) * 100 : 0))
const descripcionCuentas = computed(() => {
  const alertas = []
  if (cuentasEnMora.value > 0) alertas.push(`${cuentasEnMora.value} en mora`)
  if (cuentasBloqueadas.value > 0) alertas.push(`${cuentasBloqueadas.value} con tarjeta bloqueada`)
  return alertas.length > 0 ? alertas.join(' · ') : 'Una cuenta por categoría de cada cliente'
})

const cuentasFiltradas = computed(() => {
  const texto = textoBusqueda.value.trim().toLowerCase()
  return cuentas.value.filter((cuenta) => {
    if (ambitoFiltro.value && (cuenta.estadoCuenta ?? ESTADO_CUENTA_ACTIVA) !== ambitoFiltro.value) return false
    if (tipoFiltro.value && cuenta.tipoTarjeta !== tipoFiltro.value) return false
    if (estadoFiltro.value === ESTADO_TARJETA_BLOQUEADA ? !tarjetaVigenteBloqueada(cuenta) : estadoFiltro.value && cuenta.estadoPago !== estadoFiltro.value) return false
    if (!texto) return true
    const ultimosCuentaYTarjeta = `${obtenerUltimosCuatroDigitos(cuenta.numeroCuentaEnmascarado)} ${obtenerUltimosCuatroDigitos(cuenta.tarjetaVigente?.numeroTarjetaEnmascarado)}`
    return `${cuenta.nombreCliente} ${cuenta.codigoCliente} ${ultimosCuentaYTarjeta}`.toLowerCase().includes(texto)
  })
})
const { paginaActual, elementosDePagina: cuentasDePagina } = usarPaginacion(cuentasFiltradas)

async function cargarCuentas() {
  cargando.value = true
  mensajeError.value = ''
  try {
    ;[cuentas.value] = await Promise.all([listarCuentas(), cargarCatalogo()])
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

onMounted(cargarCuentas)
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Gestión de tarjetas' }, { texto: 'Cuentas y tarjetas' }]"
      titulo="Cuentas y tarjetas"
      descripcion="Consulta los límites, la deuda actual y la tarjeta vigente de cada cuenta de crédito."
    />

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

    <div class="rejilla-indicadores">
      <TarjetaIndicador titulo="Cuentas activas" :valor="String(cuentasActivas.length)" :descripcion="descripcionCuentas" icono="tarjeta" />
      <TarjetaIndicador titulo="Límite total" :valor="formatearMoneda(limiteTotal)" descripcion="Crédito autorizado" />
      <TarjetaIndicador titulo="Saldo / deuda total" :valor="formatearMoneda(deudaTotal)" :descripcion="`${formatearPorcentaje(porcentajeUtilizado)} del crédito utilizado`" />
      <TarjetaIndicador titulo="Disponible total" :valor="formatearMoneda(disponibleTotal)" descripcion="Límite menos saldo actual" destacado />
    </div>

    <div class="barra-herramientas">
      <SelectorPestanas v-model="ambitoFiltro" :opciones="opcionesAmbito" etiqueta-grupo="Filtrar por estado de la cuenta" />
      <div class="buscador">
        <IconoSistema nombre="buscar" :tamano="18" />
        <input v-model="textoBusqueda" type="search" placeholder="Buscar titular, código o últimos 4 dígitos" aria-label="Buscar cuentas" />
      </div>
      <select v-model="tipoFiltro" class="selector-filtro" aria-label="Filtrar por tipo de cuenta">
        <option value="">Todos los tipos</option>
        <option v-for="tipo in tiposTarjeta" :key="tipo.codigo" :value="tipo.codigo">{{ tipo.descripcion }}</option>
      </select>
      <select v-model="estadoFiltro" class="selector-filtro" aria-label="Filtrar por estado de pago">
        <option value="">Todos los estados de pago</option>
        <option v-for="estado in ESTADOS_PAGO" :key="estado.valor" :value="estado.valor">{{ estado.etiqueta }}</option>
        <option :value="ESTADO_TARJETA_BLOQUEADA">Tarjeta bloqueada</option>
      </select>
    </div>

    <section class="panel">
      <EsqueletoCarga v-if="cargando" variante="tabla" etiqueta="Cargando cuentas" />
      <p v-else-if="cuentasFiltradas.length === 0" class="estado-vacio">
        {{ cuentas.length === 0 ? 'Aún no hay cuentas abiertas.' : 'Ninguna cuenta coincide con la búsqueda.' }}
      </p>
      <template v-else>
        <div class="contenedor-tabla">
          <table class="tabla tabla-compacta">
            <thead>
              <tr>
                <th>Cliente</th>
                <th>Cuenta / tarjeta</th>
                <th>Tipo</th>
                <th>Límite</th>
                <th>Saldo</th>
                <th>Disponible</th>
                <th>Estado</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cuenta in cuentasDePagina" :key="cuenta.id">
                <td>
                  <p>{{ cuenta.nombreCliente }}</p>
                  <p class="tabla-secundaria">Código <span class="codigo-cliente">{{ cuenta.codigoCliente }}</span></p>
                </td>
                <td>
                  <p class="numero-con-etiqueta"><span class="tabla-secundaria">Cuenta</span><NumeroProtegido :enmascarado="cuenta.numeroCuentaEnmascarado" :completo="cuenta.numeroCuentaCompleto" /></p>
                  <p class="numero-con-etiqueta"><span class="tabla-secundaria">Tarjeta</span><NumeroProtegido :enmascarado="cuenta.tarjetaVigente?.numeroTarjetaEnmascarado" :completo="cuenta.tarjetaVigente?.numeroTarjetaCompleto" /></p>
                </td>
                <td><IndicadorTipoTarjeta :codigo="cuenta.tipoTarjeta" :descripcion="cuenta.descripcionTipoTarjeta" /></td>
                <td>{{ formatearMoneda(cuenta.limiteCredito) }}</td>
                <td>{{ formatearMoneda(cuenta.saldoActual) }}</td>
                <td class="celda-disponible">{{ formatearMoneda(cuenta.creditoDisponible) }}</td>
                <td><InsigniasEstadoCuenta :cuenta="cuenta" /></td>
                <td>
                  <RouterLink class="enlace-accion" :to="`/cuentas/${cuenta.id}`" :aria-label="`Ver detalle de la cuenta de ${cuenta.nombreCliente}`">Ver<IconoSistema nombre="flecha-derecha" :tamano="16" /></RouterLink>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <Paginacion v-model:pagina-actual="paginaActual" :total="cuentasFiltradas.length" etiqueta-singular="cuenta" etiqueta-plural="cuentas" />
      </template>
    </section>

    <AvisoInformativo titulo="El crédito vive en la cuenta; la tarjeta es el plástico">
      Cada solicitud aprobada abre una cuenta con su primera tarjeta. El límite es el monto aprobado y el saldo inicial es Q 0.00.
      Si la tarjeta se pierde o se daña, se repone sin cambiar la cuenta, su saldo ni su historial.
    </AvisoInformativo>
  </div>
</template>

<style scoped>
.numero-con-etiqueta { display: flex; align-items: center; gap: 4px; }
.numero-con-etiqueta > .tabla-secundaria { width: 38px; margin-top: 0; }
.rejilla-indicadores { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 24px; }
@media (max-width: 1100px) { .rejilla-indicadores { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 600px) { .rejilla-indicadores { grid-template-columns: 1fr; } }
</style>
