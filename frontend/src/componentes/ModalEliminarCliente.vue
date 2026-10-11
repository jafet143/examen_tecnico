<script setup>
import { ref, watch, computed } from 'vue'
import VentanaModal from './VentanaModal.vue'
import IconoSistema from './IconoSistema.vue'
import AvisoInformativo from './AvisoInformativo.vue'
import MensajeAlerta from './MensajeAlerta.vue'
import { eliminarCliente, listarSolicitudesDeCliente } from '../servicios/clientesServicio'
import { listarCuentasDeCliente } from '../servicios/cuentasServicio'
import { formatearMoneda, formatearCodigoSolicitud, formatearNumeroEnmascarado, obtenerIniciales } from '../utilidades/formatos'
import { obtenerMensajeError } from '../utilidades/errores'
import { ESTADO_CUENTA_ACTIVA } from '../utilidades/constantes'

const props = defineProps({
  cliente: { type: Object, default: null }
})
const emit = defineEmits(['cerrar', 'eliminado'])

const solicitudes = ref([])
const cuentas = ref([])
const cargandoRegistros = ref(false)
const eliminando = ref(false)
const mensajeError = ref('')

const visible = computed(() => props.cliente !== null)
const totalTarjetas = computed(() => cuentas.value.reduce((total, cuenta) => total + 1 + Number(cuenta.cantidadReposiciones ?? 0), 0))
const cuentasActivas = computed(() => cuentas.value.filter((cuenta) => cuenta.estadoCuenta === ESTADO_CUENTA_ACTIVA))
const tieneCuentasActivas = computed(() => cuentasActivas.value.length > 0)

async function cargarRegistrosAsociados(idCliente) {
  cargandoRegistros.value = true
  try {
    ;[solicitudes.value, cuentas.value] = await Promise.all([
      listarSolicitudesDeCliente(idCliente),
      listarCuentasDeCliente(idCliente)
    ])
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargandoRegistros.value = false
  }
}

watch(
  () => props.cliente,
  (clienteNuevo) => {
    solicitudes.value = []
    cuentas.value = []
    mensajeError.value = ''
    if (clienteNuevo) cargarRegistrosAsociados(clienteNuevo.id)
  },
  { immediate: true }
)

function cerrar() {
  if (!eliminando.value) emit('cerrar')
}

async function confirmarEliminacion() {
  mensajeError.value = ''
  eliminando.value = true
  try {
    await eliminarCliente(props.cliente.id)
    emit('eliminado', props.cliente)
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    eliminando.value = false
  }
}
</script>

<template>
  <VentanaModal :visible="visible" titulo="Eliminar cliente" descripcion="Revisa los registros asociados antes de confirmar." @cerrar="cerrar">
    <div v-if="cliente" class="modal-cuerpo">
      <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

      <div class="modal-resumen resumen-cliente">
        <span class="avatar avatar-mediano">{{ obtenerIniciales(cliente.nombre, cliente.apellido) }}</span>
        <div>
          <strong>{{ cliente.nombreCompleto }}</strong>
          <p class="tabla-secundaria">Identificación: {{ cliente.numeroIdentificacion }}</p>
        </div>
      </div>

      <AvisoInformativo v-if="!cargandoRegistros && tieneCuentasActivas" variante="error" titulo="No se puede eliminar este cliente">
        Tiene {{ cuentasActivas.length }} {{ cuentasActivas.length === 1 ? 'cuenta activa' : 'cuentas activas' }}. Para eliminarlo, primero cancela
        {{ cuentasActivas.length === 1 ? 'su cuenta' : 'todas sus cuentas' }} desde el detalle de cada cuenta.
      </AvisoInformativo>
      <AvisoInformativo v-else variante="error" titulo="También se eliminarán sus solicitudes, cuentas y tarjetas">
        Eliminar a este cliente elimina todas sus solicitudes, cuentas canceladas, tarjetas (incluidas las reemplazadas) y movimientos. Esta acción no se puede deshacer.
      </AvisoInformativo>

      <p v-if="cargandoRegistros" class="estado-carga"><span class="indicador-carga"></span>Consultando registros asociados...</p>
      <div v-else class="tarjetas-eliminacion">
        <div class="tarjeta-eliminacion">
          <IconoSistema nombre="documento" />
          <h3>{{ solicitudes.length }} {{ solicitudes.length === 1 ? 'solicitud' : 'solicitudes' }}</h3>
          <p v-for="solicitud in solicitudes" :key="solicitud.id">
            {{ formatearCodigoSolicitud(solicitud.id) }} · {{ solicitud.descripcionTipoTarjeta }}, {{ solicitud.estado.toLowerCase() }}
          </p>
        </div>
        <div class="tarjeta-eliminacion">
          <IconoSistema nombre="tarjeta" />
          <h3>
            {{ cuentas.length }} {{ cuentas.length === 1 ? 'cuenta' : 'cuentas' }} · {{ totalTarjetas }} {{ totalTarjetas === 1 ? 'tarjeta' : 'tarjetas' }}
          </h3>
          <template v-for="cuenta in cuentas" :key="cuenta.id">
            <p>
              {{ cuenta.descripcionTipoTarjeta }} · cuenta {{ formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado) }} ·
              {{ cuenta.estadoCuenta === ESTADO_CUENTA_ACTIVA ? 'activa' : 'cancelada' }}
            </p>
            <p>Deuda actual: {{ formatearMoneda(cuenta.saldoActual) }}</p>
          </template>
        </div>
      </div>

      <p v-if="!tieneCuentasActivas" class="texto-confirmacion">¿Confirmas que deseas eliminar este cliente y sus registros asociados?</p>
    </div>

    <div class="modal-pie">
      <button type="button" class="boton boton-secundario" :disabled="eliminando" @click="cerrar">Cancelar</button>
      <button type="button" class="boton boton-peligro" :disabled="eliminando || cargandoRegistros || tieneCuentasActivas" @click="confirmarEliminacion">
        <IconoSistema nombre="papelera" :tamano="18" />
        {{ eliminando ? 'Eliminando...' : 'Eliminar cliente' }}
      </button>
    </div>
  </VentanaModal>
</template>

<style scoped>
.resumen-cliente { display: flex; align-items: center; gap: 16px; }
.resumen-cliente strong { font-weight: 500; font-size: 15px; }
.texto-confirmacion { font-size: 13px; color: var(--color-texto-suave); }
</style>
