import Keycloak from 'keycloak-js'
import { ETIQUETAS_PERFIL } from '../utilidades/permisos'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENTE_ID
})

export function iniciarSesion() {
  return keycloak.login()
}

export async function inicializarAutenticacion() {
  const autenticado = await keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    checkLoginIframe: false
  })

  keycloak.onTokenExpired = () => {
    keycloak.updateToken(30).catch(() => keycloak.login())
  }

  return autenticado
}

export async function obtenerTokenActualizado() {
  try {
    await keycloak.updateToken(30)
  } catch {
    await keycloak.login()
  }
  return keycloak.token
}

function dividirNombreCompleto(nombreCompleto) {
  const palabras = nombreCompleto.trim().split(/\s+/)
  return { nombre: palabras[0], apellido: palabras.length > 1 ? palabras[palabras.length - 1] : '' }
}

export function obtenerDatosUsuario() {
  const token = keycloak.tokenParsed ?? {}
  if (token.given_name || token.family_name) {
    const nombre = token.given_name ?? ''
    const apellido = token.family_name ?? ''
    return { nombreCompleto: `${nombre} ${apellido}`.trim(), nombre, apellido }
  }
  const nombreCompleto = token.name || token.preferred_username || ''
  return { nombreCompleto, ...dividirNombreCompleto(nombreCompleto) }
}

export function obtenerRoles() {
  return keycloak.tokenParsed?.realm_access?.roles ?? []
}

export function tienePermiso(permiso) {
  return obtenerRoles().includes(permiso)
}

export function tieneAlgunPermiso(permisos) {
  return permisos.some(tienePermiso)
}

export function obtenerPerfil() {
  const perfil = Object.keys(ETIQUETAS_PERFIL).find((codigoPerfil) => obtenerRoles().includes(codigoPerfil))
  return perfil ? ETIQUETAS_PERFIL[perfil] : 'Sin perfil asignado'
}

export function cerrarSesion() {
  return keycloak.logout({ redirectUri: window.location.origin })
}
