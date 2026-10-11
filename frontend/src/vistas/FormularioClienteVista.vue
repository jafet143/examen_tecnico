<script setup>
import { reactive, ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EsqueletoCarga from '../componentes/EsqueletoCarga.vue'
import EncabezadoPagina from '../componentes/EncabezadoPagina.vue'
import CampoFormulario from '../componentes/CampoFormulario.vue'
import AvisoInformativo from '../componentes/AvisoInformativo.vue'
import IconoSistema from '../componentes/IconoSistema.vue'
import MensajeAlerta from '../componentes/MensajeAlerta.vue'
import { obtenerCliente, crearCliente, actualizarCliente } from '../servicios/clientesServicio'
import { usarNotificacion } from '../composables/usarNotificacion'
import { obtenerMensajeError, obtenerErroresDeCampos } from '../utilidades/errores'

const ruta = useRoute()
const enrutador = useRouter()
const { mostrarNotificacion } = usarNotificacion()

const idCliente = computed(() => ruta.params.idCliente)
const esEdicion = computed(() => Boolean(idCliente.value))
const tituloPagina = computed(() => (esEdicion.value ? 'Editar cliente' : 'Agregar cliente'))

const formulario = reactive({
  nombre: '',
  apellido: '',
  numeroIdentificacion: '',
  fechaNacimiento: '',
  direccion: '',
  correoElectronico: '',
  telefono: ''
})
const codigoCliente = ref('')
const erroresCampos = ref({})
const mensajeError = ref('')
const cargando = ref(false)
const enviando = ref(false)
const identificacionDuplicada = ref('')

const identificacionPendienteDeCorregir = computed(
  () => identificacionDuplicada.value !== '' && formulario.numeroIdentificacion.trim() === identificacionDuplicada.value
)

const hoy = new Date().toLocaleDateString('en-CA')

function calcularEdad(fechaNacimiento) {
  const nacimiento = new Date(`${fechaNacimiento}T00:00:00`)
  const ahora = new Date()
  let edad = ahora.getFullYear() - nacimiento.getFullYear()
  const cumpleEsteAnio = new Date(ahora.getFullYear(), nacimiento.getMonth(), nacimiento.getDate())
  if (ahora < cumpleEsteAnio) edad -= 1
  return edad
}

function validar() {
  const errores = {}
  if (!formulario.nombre.trim()) errores.nombre = 'El nombre es obligatorio'
  else if (formulario.nombre.length > 100) errores.nombre = 'El nombre no puede exceder 100 caracteres'
  if (!formulario.apellido.trim()) errores.apellido = 'El apellido es obligatorio'
  else if (formulario.apellido.length > 100) errores.apellido = 'El apellido no puede exceder 100 caracteres'
  if (!/^[A-Za-z0-9-]{5,20}$/.test(formulario.numeroIdentificacion.trim())) {
    errores.numeroIdentificacion = 'Use entre 5 y 20 letras, números o guiones'
  }
  if (!formulario.fechaNacimiento) errores.fechaNacimiento = 'La fecha de nacimiento es obligatoria'
  else if (formulario.fechaNacimiento >= hoy) errores.fechaNacimiento = 'La fecha de nacimiento debe ser pasada'
  else if (calcularEdad(formulario.fechaNacimiento) < 18) errores.fechaNacimiento = 'El cliente debe ser mayor de edad (18 años o más)'
  if (!formulario.direccion.trim()) errores.direccion = 'La dirección es obligatoria'
  else if (formulario.direccion.length > 250) errores.direccion = 'La dirección no puede exceder 250 caracteres'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formulario.correoElectronico.trim())) {
    errores.correoElectronico = 'El correo electrónico no tiene un formato válido'
  }
  if (!/^[0-9+()\- ]{8,20}$/.test(formulario.telefono.trim())) {
    errores.telefono = 'El teléfono debe tener entre 8 y 20 caracteres (números, +, (), - y espacios)'
  }
  erroresCampos.value = errores
  return Object.keys(errores).length === 0
}

watch(
  () => formulario.numeroIdentificacion,
  () => {
    if (!identificacionPendienteDeCorregir.value && erroresCampos.value.numeroIdentificacion) {
      erroresCampos.value = { ...erroresCampos.value, numeroIdentificacion: '' }
    }
  }
)

async function cargarCliente() {
  cargando.value = true
  try {
    const cliente = await obtenerCliente(idCliente.value)
    codigoCliente.value = cliente.codigoCliente
    Object.keys(formulario).forEach((campo) => {
      formulario[campo] = cliente[campo] ?? ''
    })
  } catch (error) {
    mensajeError.value = obtenerMensajeError(error)
  } finally {
    cargando.value = false
  }
}

async function guardarCliente() {
  mensajeError.value = ''
  if (!validar()) return
  enviando.value = true
  try {
    const datos = {}
    Object.keys(formulario).forEach((campo) => {
      datos[campo] = formulario[campo].trim()
    })
    const clienteGuardado = esEdicion.value
      ? await actualizarCliente(idCliente.value, datos)
      : await crearCliente(datos)
    mostrarNotificacion(esEdicion.value ? 'Cliente actualizado correctamente' : 'Cliente registrado correctamente')
    enrutador.push(`/clientes/${clienteGuardado.id}`)
  } catch (error) {
    const mensaje = obtenerMensajeError(error)
    const erroresRecibidos = obtenerErroresDeCampos(error)
    if (error.response?.status === 409 && !erroresRecibidos.numeroIdentificacion) {
      erroresRecibidos.numeroIdentificacion = mensaje
      identificacionDuplicada.value = formulario.numeroIdentificacion.trim()
    } else {
      mensajeError.value = mensaje
    }
    erroresCampos.value = erroresRecibidos
  } finally {
    enviando.value = false
  }
}

onMounted(() => {
  if (esEdicion.value) cargarCliente()
})
</script>

<template>
  <div>
    <EncabezadoPagina
      :migas="[{ texto: 'Clientes', ruta: '/clientes' }, { texto: tituloPagina }]"
      :titulo="tituloPagina"
      descripcion="Completa los datos personales y de contacto. Todos los campos son obligatorios."
      :ruta-regreso="esEdicion ? `/clientes/${idCliente}` : '/clientes'"
    />

    <MensajeAlerta :mensaje="mensajeError" tipo="error" @cerrar="mensajeError = ''" />

    <EsqueletoCarga v-if="cargando" variante="formulario" etiqueta="Cargando cliente" />
    <div v-else class="rejilla-con-lateral">
      <form class="panel" novalidate @submit.prevent="guardarCliente">
        <div class="panel-cabecera">
          <div>
            <h2 class="panel-titulo">Datos del cliente</h2>
            <p class="panel-subtitulo">{{ esEdicion ? 'Editando registro' : 'Nuevo registro' }}</p>
          </div>
        </div>
        <div class="panel-cuerpo">
          <div class="rejilla-campos">
            <CampoFormulario v-if="esEdicion" id="codigoCliente" etiqueta="Código de cliente" class="campo-ancho-completo" ayuda="Lo asigna el sistema y no se puede modificar">
              <input id="codigoCliente" :value="codigoCliente" type="text" readonly />
            </CampoFormulario>
            <CampoFormulario id="nombre" etiqueta="Nombre *" :error="erroresCampos.nombre">
              <input id="nombre" v-model="formulario.nombre" type="text" maxlength="100" />
            </CampoFormulario>
            <CampoFormulario id="apellido" etiqueta="Apellido *" :error="erroresCampos.apellido">
              <input id="apellido" v-model="formulario.apellido" type="text" maxlength="100" />
            </CampoFormulario>
            <CampoFormulario id="numeroIdentificacion" etiqueta="Número de identificación *" :error="erroresCampos.numeroIdentificacion">
              <input id="numeroIdentificacion" v-model="formulario.numeroIdentificacion" type="text" maxlength="20" />
            </CampoFormulario>
            <CampoFormulario id="fechaNacimiento" etiqueta="Fecha de nacimiento *" :error="erroresCampos.fechaNacimiento" ayuda="Día / mes / año">
              <input id="fechaNacimiento" v-model="formulario.fechaNacimiento" type="date" :max="hoy" />
            </CampoFormulario>
            <CampoFormulario id="direccion" etiqueta="Dirección *" :error="erroresCampos.direccion" class="campo-ancho-completo">
              <input id="direccion" v-model="formulario.direccion" type="text" maxlength="250" />
            </CampoFormulario>
            <CampoFormulario id="correoElectronico" etiqueta="Correo electrónico *" :error="erroresCampos.correoElectronico">
              <input id="correoElectronico" v-model="formulario.correoElectronico" type="email" maxlength="150" />
            </CampoFormulario>
            <CampoFormulario id="telefono" etiqueta="Teléfono *" :error="erroresCampos.telefono">
              <input id="telefono" v-model="formulario.telefono" type="tel" maxlength="20" />
            </CampoFormulario>
          </div>

          <AvisoInformativo v-if="identificacionPendienteDeCorregir" variante="error" titulo="Revisa la identificación antes de guardar">
            Los demás datos están completos. Corrige el número duplicado para continuar.
          </AvisoInformativo>
        </div>
        <div class="panel-pie">
          <span>{{ esEdicion ? 'Los cambios aún no se han guardado.' : 'El registro aún no se ha guardado.' }}</span>
          <div class="acciones-pie">
            <RouterLink class="boton boton-secundario" :to="esEdicion ? `/clientes/${idCliente}` : '/clientes'">Cancelar</RouterLink>
            <button type="submit" class="boton" :disabled="enviando || identificacionPendienteDeCorregir">
              {{ enviando ? 'Guardando...' : 'Guardar cliente' }}
            </button>
          </div>
        </div>
      </form>

      <aside class="panel">
        <div class="panel-cabecera"><h2 class="panel-titulo">Un registro por persona</h2></div>
        <div class="panel-cuerpo avisos-apilados">
          <AvisoInformativo titulo="Identificación única">No pueden existir dos clientes con el mismo número de identificación.</AvisoInformativo>
          <p class="nota-lateral"><IconoSistema nombre="contacto" />Verifica el correo y el teléfono para mantener la información de contacto actualizada.</p>
          <p class="nota-lateral nota-pequena">Para editar un cliente, estos mismos campos se muestran con sus datos actuales.</p>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.acciones-pie { display: flex; gap: 10px; }
.nota-lateral { display: flex; gap: 10px; align-items: flex-start; font-size: 13px; line-height: 1.5; color: var(--color-texto-suave); }
.nota-lateral .icono { flex: none; color: var(--color-primario); }
.nota-pequena { font-size: 12px; }
</style>
