<script setup>
import CampoMoneda from '../componentes/CampoMoneda.vue'
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import CampoFormulario from '../componentes/CampoFormulario.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import EtiquetaEstado from '../componentes/EtiquetaEstado.vue'
import { listarClientes } from '../servicios/clientesServicio'
import { crearSolicitud } from '../servicios/solicitudesServicio'
import { listarCuentasDeCliente } from '../servicios/cuentasServicio'
import { ESTADO_CUENTA_CANCELADA } from '../utilidades/constantes'
import { usarNotificacion } from '../composables/usarNotificacion'
import { usarCatalogoTiposTarjeta } from '../composables/usarCatalogoTiposTarjeta'
import { formatearFechaHoy, formatearFecha, formatearMoneda, formatearNumeroEnmascarado, calcularMesesAntiguedad, describirAntiguedad, obtenerFechaIsoHoy } from '../utilidades/formatos'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const ruta = useRoute()
const enrutador = useRouter()
const { mostrarNotificacion } = usarNotificacion()
const { tiposTarjeta, mesesMinimosEstabilidadLaboral, errorCatalogo, cargarCatalogo } = usarCatalogoTiposTarjeta()

const idClientePreseleccionado = ruta.query.idCliente ? Number(ruta.query.idCliente) : null
const rutaAlCancelar = idClientePreseleccionado ? `/clientes/${idClientePreseleccionado}` : '/solicitudes'

const clientes = ref([])
const formulario = reactive({
  idCliente: idClientePreseleccionado ?? '',
  tipoTarjeta: 'CLASICA',
  ingresoMensual: '',
  nombreEmpleador: '',
  fechaInicioLaboral: ''
})
const cuentasDelCliente = ref([])
const fechaMaxima = obtenerFechaIsoHoy()
const erroresCampos = ref({})
const mensajeError = ref('')
const cargando = ref(true)
const enviando = ref(false)

const clienteSeleccionado = computed(() => clientes.value.find((cliente) => cliente.id === formulario.idCliente) ?? null)
const clienteEnMora = computed(() => clienteSeleccionado.value?.tieneCuentasEnMora === true)

const ingresoDeclarado = computed(() => Number(formulario.ingresoMensual))
const hayIngresoDeclarado = computed(() => formulario.ingresoMensual !== '' && ingresoDeclarado.value > 0)
const cuentasActivasDelCliente = computed(() => cuentasDelCliente.value.filter((cuenta) => cuenta.estadoCuenta !== ESTADO_CUENTA_CANCELADA))
const cuentasCanceladasDelCliente = computed(() => cuentasDelCliente.value.filter((cuenta) => cuenta.estadoCuenta === ESTADO_CUENTA_CANCELADA))
const cuentaExistenteDelTipo = (codigoTipo) => cuentasActivasDelCliente.value.find((cuenta) => cuenta.tipoTarjeta === codigoTipo) ?? null
const tiposConDisponibilidad = computed(() =>
  tiposTarjeta.value.map((tipo) => ({
    ...tipo,
    tieneCuenta: cuentaExistenteDelTipo(tipo.codigo) !== null,
    disponible: !hayIngresoDeclarado.value || ingresoDeclarado.value >= Number(tipo.ingresoMinimo)
  }))
)

const mesesAntiguedad = computed(() => calcularMesesAntiguedad(formulario.fechaInicioLaboral))
const fechaInicioFutura = computed(() => formulario.fechaInicioLaboral > fechaMaxima)
const estabilidadInsuficiente = computed(
  () => mesesAntiguedad.value !== null && !fechaInicioFutura.value && mesesMinimosEstabilidadLaboral.value !== null && mesesAntiguedad.value < mesesMinimosEstabilidadLaboral.value
)
const errorFechaInicio = computed(() => {
  if (erroresCampos.value.fechaInicioLaboral) return erroresCampos.value.fechaInicioLaboral
  if (fechaInicioFutura.value) return 'La fecha de inicio laboral no puede ser futura'
  if (estabilidadInsuficiente.value) return 'Se requiere al menos 1 año de estabilidad laboral'
  return ''
})
const ayudaFechaInicio = computed(() =>
  mesesAntiguedad.value !== null && !fechaInicioFutura.value ? `Antigüedad: ${describirAntiguedad(mesesAntiguedad.value)}` : 'Desde cuándo trabaja con el empleador actual'
)
const tipoSeleccionado = computed(() => tiposConDisponibilidad.value.find((tipo) => tipo.codigo === formulario.tipoTarjeta) ?? null)
const errorTipoTarjeta = computed(() => {
  if (erroresCampos.value.tipoTarjeta) return erroresCampos.value.tipoTarjeta
  if (tipoSeleccionado.value?.tieneCuenta) return `Ya tiene cuenta ${tipoSeleccionado.value.descripcion}`
  if (tipoSeleccionado.value && !tipoSeleccionado.value.disponible) {
    return `El ingreso de ${formatearMoneda(ingresoDeclarado.value)} no alcanza el mínimo de ${formatearMoneda(tipoSeleccionado.value.ingresoMinimo)} para la tarjeta ${tipoSeleccionado.value.descripcion}`
  }
  return ''
})

function validar() {
  const errores = {}
  if (!formulario.idCliente) errores.idCliente = 'Seleccione un cliente'
  if (!formulario.tipoTarjeta) errores.tipoTarjeta = 'Seleccione el tipo de tarjeta'
  else if (errorTipoTarjeta.value) errores.tipoTarjeta = errorTipoTarjeta.value
  if (!(Number(formulario.ingresoMensual) > 0)) errores.ingresoMensual = 'El ingreso mensual debe ser mayor a cero'
  const empleador = formulario.nombreEmpleador.trim()
  if (!empleador) errores.nombreEmpleador = 'El empleador es obligatorio'
  else if (empleador.length < 2 || empleador.length > 150) errores.nombreEmpleador = 'El empleador debe tener entre 2 y 150 caracteres'
  if (!formulario.fechaInicioLaboral) errores.fechaInicioLaboral = 'La fecha de inicio laboral es obligatoria'
  else if (errorFechaInicio.value) errores.fechaInicioLaboral = errorFechaInicio.value
  erroresCampos.value = errores
  return Object.keys(errores).length === 0
}

async function cargarCuentasDelCliente(idCliente) {
  cuentasDelCliente.value = []
  if (!idCliente) return
  try {
    cuentasDelCliente.value = await listarCuentasDeCliente(idCliente)
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  }
  if (cuentaExistenteDelTipo(formulario.tipoTarjeta)) {
    formulario.tipoTarjeta = tiposConDisponibilidad.value.find((tipo) => !tipo.tieneCuenta)?.codigo ?? ''
  }
}

watch(() => formulario.idCliente, cargarCuentasDelCliente)

async function cargarClientes() {
  try {
    ;[clientes.value] = await Promise.all([listarClientes(), cargarCatalogo()])
    if (errorCatalogo.value) mensajeError.value = errorCatalogo.value
    await cargarCuentasDelCliente(formulario.idCliente)
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

async function guardarSolicitud() {
  mensajeError.value = ''
  if (clienteEnMora.value || !validar()) return
  enviando.value = true
  try {
    await crearSolicitud({
      idCliente: formulario.idCliente,
      tipoTarjeta: formulario.tipoTarjeta,
      ingresoMensual: Number(formulario.ingresoMensual),
      nombreEmpleador: formulario.nombreEmpleador.trim(),
      fechaInicioLaboral: formulario.fechaInicioLaboral
    })
    mostrarNotificacion('Solicitud registrada correctamente')
    enrutador.push(rutaAlCancelar)
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
    erroresCampos.value = obtenerErroresDeCampos(error)
  } finally {
    enviando.value = false
  }
}

onMounted(cargarClientes)
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Solicitudes', ruta: '/solicitudes' }, { texto: 'Nueva solicitud' }]"
      titulo="Nueva solicitud"
      descripcion="Registra el cliente, el tipo de cuenta, el ingreso mensual y la información laboral."
      ruta-regreso="/solicitudes"
    />

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

    <EsqueletoCarga v-if="cargando" variante="formulario" etiqueta="Cargando clientes" />
    <div v-else class="rejilla-con-lateral">
      <form class="panel" novalidate @submit.prevent="guardarSolicitud">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Datos de la solicitud</h2>
            <p class="panel-subtitulo">Todos los campos son obligatorios</p>
          </div>
        </div>
        <div class="panel-cuerpo campos-solicitud">
          <CampoFormulario
            id="idCliente"
            etiqueta="Cliente *"
            :error="erroresCampos.idCliente"
            :ayuda="clienteSeleccionado ? `Identificación: ${clienteSeleccionado.numeroIdentificacion}` : ''"
          >
            <select id="idCliente" v-model="formulario.idCliente">
              <option value="" disabled>Selecciona un cliente</option>
              <option v-for="cliente in clientes" :key="cliente.id" :value="cliente.id">{{ cliente.nombreCompleto }} · {{ cliente.codigoCliente }}</option>
            </select>
          </CampoFormulario>

          <AvisoInformativo v-if="clienteEnMora" variante="error" titulo="El cliente tiene cuentas en mora">
            Debe ponerse al día en sus pagos antes de solicitar una nueva tarjeta.
          </AvisoInformativo>

          <fieldset class="grupo-tipo" :class="{ 'con-error': errorTipoTarjeta }">
            <legend>Tipo de tarjeta *</legend>
            <div class="opciones-tarjeta">
              <label
                v-for="tipo in tiposConDisponibilidad"
                :key="tipo.codigo"
                class="opcion-tarjeta"
                :class="{ seleccionada: formulario.tipoTarjeta === tipo.codigo, deshabilitada: !tipo.disponible || tipo.tieneCuenta }"
              >
                <input v-model="formulario.tipoTarjeta" type="radio" name="tipoTarjeta" :value="tipo.codigo" :disabled="!tipo.disponible || tipo.tieneCuenta" />
                <IconoSistema nombre="tarjeta" />
                <span class="opcion-tarjeta-texto">
                  {{ tipo.descripcion }}
                  <span class="opcion-tarjeta-detalle">
                    {{
                      tipo.tieneCuenta
                        ? `Ya tiene cuenta ${tipo.descripcion}`
                        : tipo.disponible
                          ? `Ingreso mínimo ${formatearMoneda(tipo.ingresoMinimo)}`
                          : `Requiere ingreso de ${formatearMoneda(tipo.ingresoMinimo)}`
                    }}
                  </span>
                </span>
                <IconoSistema v-if="formulario.tipoTarjeta === tipo.codigo" class="icono-seleccion" nombre="check-circulo" :tamano="18" />
              </label>
            </div>
            <p v-if="errorTipoTarjeta" class="mensaje-error"><IconoSistema nombre="alerta" :tamano="14" />{{ errorTipoTarjeta }}</p>
          </fieldset>

          <div class="rejilla-campos">
            <CampoFormulario id="fechaSolicitud" etiqueta="Fecha de solicitud *" ayuda="Se registra automáticamente al guardar">
              <input id="fechaSolicitud" type="text" :value="formatearFechaHoy()" readonly />
            </CampoFormulario>
            <CampoFormulario id="ingresoMensual" etiqueta="Ingreso mensual declarado *" :error="erroresCampos.ingresoMensual" ayuda="Monto mensual en quetzales">
              <CampoMoneda id="ingresoMensual" v-model="formulario.ingresoMensual" />
            </CampoFormulario>
          </div>

          <h3 class="subtitulo-seccion">Información laboral</h3>
          <div class="rejilla-campos">
            <CampoFormulario id="nombreEmpleador" etiqueta="Empleador *" :error="erroresCampos.nombreEmpleador">
              <input id="nombreEmpleador" v-model="formulario.nombreEmpleador" type="text" maxlength="150" autocomplete="organization" />
            </CampoFormulario>
            <CampoFormulario id="fechaInicioLaboral" etiqueta="Fecha de inicio laboral *" :error="errorFechaInicio" :ayuda="ayudaFechaInicio">
              <input id="fechaInicioLaboral" v-model="formulario.fechaInicioLaboral" type="date" :max="fechaMaxima" />
            </CampoFormulario>
          </div>

          <AvisoInformativo titulo="Estado inicial: Pendiente">
            Toda nueva solicitud queda pendiente de revisión. Registrarla no genera una cuenta, una tarjeta ni un límite de crédito.
          </AvisoInformativo>
        </div>
        <div class="panel-pie">
          <span>Solicitud aún no registrada</span>
          <div class="acciones-pie">
            <RouterLink class="boton boton-secundario" :to="rutaAlCancelar">Cancelar</RouterLink>
            <button type="submit" class="boton" :disabled="enviando || clienteEnMora || estabilidadInsuficiente" :title="clienteEnMora ? 'El cliente tiene cuentas en mora' : estabilidadInsuficiente ? 'Se requiere al menos 1 año de estabilidad laboral' : ''">{{ enviando ? 'Guardando...' : 'Crear solicitud' }}</button>
          </div>
        </div>
      </form>

      <div class="columna-lateral">
        <aside class="panel">
          <div class="panel-cabecera"><h2 class="panel-titulo">Cliente seleccionado</h2></div>
          <div class="panel-cuerpo">
            <p v-if="!clienteSeleccionado" class="ayuda">Selecciona un cliente para ver sus datos.</p>
            <div v-else class="lista-datos">
              <div><p class="dato-etiqueta">Nombre completo</p><p class="dato-valor">{{ clienteSeleccionado.nombreCompleto }}</p></div>
              <div><p class="dato-etiqueta">Código de cliente</p><p class="dato-valor codigo-cliente">{{ clienteSeleccionado.codigoCliente }}</p></div>
              <div><p class="dato-etiqueta">Identificación</p><p class="dato-valor">{{ clienteSeleccionado.numeroIdentificacion }}</p></div>
              <div><p class="dato-etiqueta">Correo electrónico</p><p class="dato-valor">{{ clienteSeleccionado.correoElectronico }}</p></div>
              <div><p class="dato-etiqueta">Teléfono</p><p class="dato-valor">{{ clienteSeleccionado.telefono }}</p></div>
              <div v-if="clienteSeleccionado.tieneCuentasEnMora !== undefined">
                <p class="dato-etiqueta">Estado de pagos</p>
                <EtiquetaEstado :estado="clienteEnMora ? 'EN_MORA' : 'AL_DIA'" />
              </div>
            </div>
          </div>
        </aside>
        <aside v-if="clienteSeleccionado" class="panel">
          <div class="panel-cabecera"><h2 class="panel-titulo">Productos del cliente</h2></div>
          <div class="panel-cuerpo lista-datos">
            <div>
              <p class="dato-etiqueta">Productos activos</p>
              <p v-if="cuentasActivasDelCliente.length === 0" class="ayuda sin-margen">Sin productos activos.</p>
              <ul v-else class="lista-productos">
                <li v-for="cuenta in cuentasActivasDelCliente" :key="cuenta.id">
                  <strong>{{ cuenta.descripcionTipoTarjeta }}</strong>
                  <span class="tabla-secundaria">Cuenta {{ formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado) }} · apertura {{ formatearFecha(cuenta.fechaApertura) }}</span>
                </li>
              </ul>
            </div>
            <div>
              <p class="dato-etiqueta">Productos anteriores</p>
              <p v-if="cuentasCanceladasDelCliente.length === 0" class="ayuda sin-margen">Sin productos anteriores.</p>
              <ul v-else class="lista-productos">
                <li v-for="cuenta in cuentasCanceladasDelCliente" :key="cuenta.id">
                  <strong>{{ cuenta.descripcionTipoTarjeta }} <EtiquetaEstado :estado="ESTADO_CUENTA_CANCELADA" etiqueta="Cancelada" class="etiqueta-pequena" /></strong>
                  <span class="tabla-secundaria">Cuenta {{ formatearNumeroEnmascarado(cuenta.numeroCuentaEnmascarado) }}</span>
                  <span class="tabla-secundaria">Apertura {{ formatearFecha(cuenta.fechaApertura) }} · cancelación {{ formatearFecha(cuenta.fechaCancelacion) }}</span>
                  <span class="tabla-secundaria">Motivo: {{ cuenta.descripcionMotivoCancelacion }}</span>
                </li>
              </ul>
            </div>
          </div>
        </aside>
        <aside class="panel">
          <div class="panel-cabecera"><h2 class="panel-titulo">Después del registro</h2></div>
          <div class="panel-cuerpo lista-datos">
            <div><EtiquetaEstado estado="PENDIENTE" /></div>
            <p class="ayuda sin-margen">Revisa la solicitud para aprobarla o rechazarla. Si se aprueba, se abre una cuenta de crédito con el monto autorizado y se emite su primera tarjeta.</p>
          </div>
        </aside>
        <aside class="panel">
          <div class="panel-cabecera"><h2 class="panel-titulo">Requisitos</h2></div>
          <ul class="panel-cuerpo lista-requisitos">
            <li>Ser mayor de edad.</li>
            <li>Al menos {{ mesesMinimosEstabilidadLaboral === 12 || mesesMinimosEstabilidadLaboral === null ? '1 año' : `${mesesMinimosEstabilidadLaboral} meses` }} de estabilidad laboral.</li>
            <li>
              Ingreso mensual mínimo del tipo<template v-if="tipoSeleccionado">: {{ formatearMoneda(tipoSeleccionado.ingresoMinimo) }} ({{ tipoSeleccionado.descripcion }})</template>.
            </li>
            <li>Sin cuentas en mora.</li>
            <li>Una cuenta activa por categoría de tarjeta.</li>
          </ul>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.campos-solicitud { display: flex; flex-direction: column; gap: 22px; }
.campos-solicitud .rejilla-campos { margin-bottom: 0; }
.acciones-pie { display: flex; gap: 10px; }
.grupo-tipo { margin: 0; padding: 0; border: 0; min-width: 0; }
.grupo-tipo legend { padding: 0; margin-bottom: 8px; font-size: 13px; font-weight: 600; }
.sin-margen { margin-top: 0; }
.lista-productos { display: flex; flex-direction: column; gap: 12px; margin: 0; padding: 0; list-style: none; }
.lista-productos li { display: flex; flex-direction: column; gap: 2px; }
.lista-productos .tabla-secundaria { margin-top: 0; }
.subtitulo-seccion { margin-bottom: -6px; font-size: 15px; font-weight: 600; }
.lista-requisitos { margin: 0; padding-left: 40px; display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--color-texto-suave); }
</style>
