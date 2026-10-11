package gt.com.chn.tarjetas.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.com.chn.tarjetas.config.ConfiguracionSeguridad;
import gt.com.chn.tarjetas.config.ConvertidorRolesKeycloak;
import gt.com.chn.tarjetas.config.ManejadorErroresSeguridad;
import gt.com.chn.tarjetas.config.Permisos;
import gt.com.chn.tarjetas.config.UsuarioActual;
import gt.com.chn.tarjetas.dto.CuentaRespuesta;
import gt.com.chn.tarjetas.dto.TarjetaRespuesta;
import gt.com.chn.tarjetas.entity.EstadoCuenta;
import gt.com.chn.tarjetas.entity.EstadoPago;
import gt.com.chn.tarjetas.entity.EstadoTarjeta;
import gt.com.chn.tarjetas.entity.TipoTarjeta;
import gt.com.chn.tarjetas.exception.ManejadorGlobalExcepciones;
import gt.com.chn.tarjetas.exception.RecursoNoEncontradoException;
import gt.com.chn.tarjetas.service.CargoService;
import gt.com.chn.tarjetas.service.ClienteService;
import gt.com.chn.tarjetas.service.CuentaService;
import gt.com.chn.tarjetas.service.MovimientoCuentaService;
import gt.com.chn.tarjetas.service.SolicitudService;
import gt.com.chn.tarjetas.service.TarjetaService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = {ClienteController.class, SolicitudController.class, TarjetaController.class,
        CuentaController.class, ReversoController.class, CatalogoController.class})
@Import({ConfiguracionSeguridad.class, ConvertidorRolesKeycloak.class, ManejadorErroresSeguridad.class,
        ManejadorGlobalExcepciones.class, UsuarioActual.class})
class SeguridadControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ClienteService clienteService;
    @MockBean
    private SolicitudService solicitudService;
    @MockBean
    private TarjetaService tarjetaService;
    @MockBean
    private CuentaService cuentaService;
    @MockBean
    private MovimientoCuentaService movimientoCuentaService;
    @MockBean
    private CargoService cargoService;
    @MockBean
    private JwtDecoder jwtDecoder;

    private static final List<String> ADMINISTRADOR = List.of(
            Permisos.CLIENTES_CONSULTAR, Permisos.CLIENTES_GESTIONAR, Permisos.CLIENTES_ELIMINAR,
            Permisos.SOLICITUDES_CONSULTAR, Permisos.SOLICITUDES_REGISTRAR, Permisos.SOLICITUDES_DECIDIR,
            Permisos.CUENTAS_CONSULTAR, Permisos.CONSUMOS_REGISTRAR, Permisos.PAGOS_REGISTRAR,
            Permisos.MOVIMIENTOS_REVERSAR, Permisos.TARJETAS_BLOQUEAR, Permisos.TARJETAS_DESBLOQUEAR,
            Permisos.TARJETAS_REPONER, Permisos.TARJETAS_RENOVAR, Permisos.CUENTAS_CANCELAR,
            Permisos.CARGOS_REGISTRAR, Permisos.CARGOS_AJUSTAR, Permisos.DATOS_SENSIBLES_VER);
    private static final List<String> OPERADOR = List.of(
            Permisos.CLIENTES_CONSULTAR, Permisos.CLIENTES_GESTIONAR, Permisos.SOLICITUDES_CONSULTAR,
            Permisos.SOLICITUDES_REGISTRAR, Permisos.CUENTAS_CONSULTAR, Permisos.CONSUMOS_REGISTRAR,
            Permisos.TARJETAS_BLOQUEAR, Permisos.TARJETAS_REPONER, Permisos.TARJETAS_RENOVAR);
    private static final List<String> CAJERO = List.of(
            Permisos.CLIENTES_CONSULTAR, Permisos.CUENTAS_CONSULTAR, Permisos.PAGOS_REGISTRAR);
    private static final List<String> ANALISTA_CREDITO = List.of(
            Permisos.CLIENTES_CONSULTAR, Permisos.SOLICITUDES_CONSULTAR, Permisos.SOLICITUDES_DECIDIR,
            Permisos.CUENTAS_CONSULTAR);

    private static final String JSON_MOVIMIENTO = "{\"monto\":100.00,\"descripcion\":\"Compra\"}";
    private static final String JSON_CARGO =
            "{\"tipoCargo\":\"RECARGO_MORA\",\"monto\":150.00,\"descripcion\":\"Recargo por mora\"}";
    private static final String JSON_CANCELACION = "{\"motivoCancelacion\":\"SOLICITUD_CLIENTE\"}";
    private static final String JSON_REVERSO = "{\"motivo\":\"Cargo duplicado por el comercio\"}";

    private RequestPostProcessor token(List<String> permisos) {
        return jwt().jwt(token -> token.claim("preferred_username", "usuario"))
                .authorities(permisos.stream()
                        .map(permiso -> new SimpleGrantedAuthority("ROLE_" + permiso))
                        .toArray(SimpleGrantedAuthority[]::new));
    }

    private RequestPostProcessor tokenConPermisos(String... permisos) {
        return token(Arrays.asList(permisos));
    }

    @Test
    @DisplayName("Sin token una ruta protegida responde 401 con formato de error")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401))
                .andExpect(jsonPath("$.mensaje").value("Debe iniciar sesión para acceder a este recurso"))
                .andExpect(jsonPath("$.errores").isMap());
    }

    @Test
    @DisplayName("OPERADOR no puede eliminar clientes: 403")
    void operadorNoPuedeEliminarCliente() throws Exception {
        mockMvc.perform(delete("/gestion-tarjetas/clientes/1").with(token(OPERADOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("No tiene permisos para realizar esta operación"));
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("OPERADOR no puede aprobar solicitudes: 403")
    void operadorNoPuedeAprobar() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/aprobar")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoAprobado\":1000.00}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(solicitudService);
    }

    @Test
    @DisplayName("OPERADOR no puede desbloquear tarjetas: 403")
    void operadorNoPuedeDesbloquear() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/desbloquear").with(token(OPERADOR)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(tarjetaService);
    }

    @Test
    @DisplayName("ADMINISTRADOR puede desbloquear y OPERADOR puede bloquear tarjetas")
    void bloquearYDesbloquearSegunRol() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/desbloquear").with(token(ADMINISTRADOR)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/bloquear")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivoBloqueo\":\"ROBO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Bloquear sin motivo responde 400")
    void bloquearSinMotivo() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/bloquear")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivoBloqueo").exists());
    }

    @Test
    @DisplayName("El catálogo lo consulta quien tenga permiso de cuentas o de solicitudes e incluye BLACK, los motivos y la estabilidad laboral")
    void catalogoTiposTarjeta() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/catalogos/tipos-tarjeta").with(token(OPERADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porcentajeMaximoLimiteSobreIngreso").value(150))
                .andExpect(jsonPath("$.diaCorte").value(5))
                .andExpect(jsonPath("$.diaLimitePago").value(25))
                .andExpect(jsonPath("$.mesesMinimosEstabilidadLaboral").value(12))
                .andExpect(jsonPath("$.tiposTarjeta[3].codigo").value("BLACK"))
                .andExpect(jsonPath("$.tiposTarjeta[3].ingresoMinimo").value(30000.00))
                .andExpect(jsonPath("$.motivosBloqueo[0].codigo").value("ROBO"))
                .andExpect(jsonPath("$.motivosBloqueo[0].permiteDesbloqueo").value(false))
                .andExpect(jsonPath("$.motivosBloqueo[2].codigo").value("SOSPECHA_FRAUDE"))
                .andExpect(jsonPath("$.motivosBloqueo[2].permiteDesbloqueo").value(false))
                .andExpect(jsonPath("$.motivosBloqueo[3].codigo").value("SOLICITUD_CLIENTE"))
                .andExpect(jsonPath("$.motivosBloqueo[3].permiteDesbloqueo").value(true))
                .andExpect(jsonPath("$.motivosReposicion[0].codigo").value("ROBO"))
                .andExpect(jsonPath("$.motivosReposicion[2].codigo").value("DETERIORO"))
                .andExpect(jsonPath("$.motivosReposicion[2].descripcion").value("Deterioro"))
                .andExpect(jsonPath("$.motivosReposicion.length()").value(4))
                .andExpect(jsonPath("$.comisionReposicionExtravio").value(45.00))
                .andExpect(jsonPath("$.porcentajePagoMinimo").value(10))
                .andExpect(jsonPath("$.tasaInteresMensual").value(5))
                .andExpect(jsonPath("$.motivosCancelacion[0].codigo").value("SOLICITUD_CLIENTE"))
                .andExpect(jsonPath("$.motivosCancelacion[1].descripcion").value("Decisión del banco"))
                .andExpect(jsonPath("$.tiposCargoManual.length()").value(1))
                .andExpect(jsonPath("$.tiposCargoManual[0].codigo").value("RECARGO_MORA"))
                .andExpect(jsonPath("$.tiposCargoManual[0].descripcion").value("Recargo por mora"));
        for (List<String> perfil : List.of(ADMINISTRADOR, CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(get("/gestion-tarjetas/catalogos/tipos-tarjeta").with(token(perfil))).andExpect(status().isOk());
        }
        mockMvc.perform(get("/gestion-tarjetas/catalogos/tipos-tarjeta").with(tokenConPermisos(Permisos.CLIENTES_CONSULTAR)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMINISTRADOR elimina cliente: 204")
    void administradorPuedeEliminarCliente() throws Exception {
        mockMvc.perform(delete("/gestion-tarjetas/clientes/1").with(token(ADMINISTRADOR)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("OPERADOR puede consultar clientes y un 404 usa el formato de error")
    void operadorConsultaClientes() throws Exception {
        when(clienteService.obtener(5L)).thenThrow(new RecursoNoEncontradoException("No se encontró el cliente con id 5"));

        mockMvc.perform(get("/gestion-tarjetas/clientes").with(token(OPERADOR)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/gestion-tarjetas/clientes/5").with(token(OPERADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No se encontró el cliente con id 5"))
                .andExpect(jsonPath("$.ruta").value("/gestion-tarjetas/clientes/5"));
    }

    @Test
    @DisplayName("Petición inválida responde 400 con errores por campo")
    void peticionInvalidaResponde400() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/clientes")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoElectronico\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La petición contiene datos inválidos"))
                .andExpect(jsonPath("$.errores.correoElectronico").exists())
                .andExpect(jsonPath("$.errores.nombre").exists());
    }

    @Test
    @DisplayName("OPERADOR no puede reversar consumos ni pagos: 403")
    void operadorNoPuedeReversar() throws Exception {
        for (String ruta : new String[] {"/gestion-tarjetas/consumos/1/reversar", "/gestion-tarjetas/pagos/1/reversar"}) {
            mockMvc.perform(put(ruta)
                            .with(token(OPERADOR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"Cargo duplicado por el comercio\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.mensaje").value("No tiene permisos para realizar esta operación"));
        }
        verifyNoInteractions(movimientoCuentaService);
    }

    @Test
    @DisplayName("ADMINISTRADOR puede reversar consumos y pagos")
    void administradorPuedeReversar() throws Exception {
        for (String ruta : new String[] {"/gestion-tarjetas/consumos/1/reversar", "/gestion-tarjetas/pagos/1/reversar"}) {
            mockMvc.perform(put(ruta)
                            .with(token(ADMINISTRADOR))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivo\":\"Cargo duplicado por el comercio\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Reversar con un motivo demasiado corto o vacío responde 400")
    void reversarMotivoInvalido() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/consumos/1/reversar")
                        .with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
        mockMvc.perform(put("/gestion-tarjetas/pagos/1/reversar")
                        .with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivo").exists());
    }

    @Test
    @DisplayName("Quien tiene TARJETAS_REPONER repone y responde 201; CAJERO y ANALISTA reciben 403; sin motivo responde 400")
    void reponerTarjetaAmbosRoles() throws Exception {
        when(tarjetaService.reponer(eq(1L), any(), any(), anyBoolean())).thenReturn(tarjetaNueva());
        for (List<String> perfil : List.of(ADMINISTRADOR, OPERADOR)) {
            mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/reposiciones")
                            .with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivoReposicion\":\"EXTRAVIO\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/gestion-tarjetas/tarjetas/9"))
                    .andExpect(jsonPath("$.id").value(9));
        }
        mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/reposiciones")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivoReposicion").exists());
        for (List<String> perfil : List.of(CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/reposiciones")
                            .with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"motivoReposicion\":\"EXTRAVIO\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Las rutas reemplazadas no tienen permiso asignado y se deniegan: GET /gestion-tarjetas/tarjetas y /gestion-tarjetas/clientes/{id}/tarjetas")
    void rutasReemplazadasNoExisten() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/tarjetas").with(token(ADMINISTRADOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion-tarjetas/clientes/1/tarjetas").with(token(ADMINISTRADOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/consumos").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Todos los perfiles con CUENTAS_CONSULTAR consultan cuentas, tarjetas y movimientos")
    void cuentasConPermisoDeConsulta() throws Exception {
        for (List<String> perfil : List.of(ADMINISTRADOR, OPERADOR, CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(get("/gestion-tarjetas/cuentas").with(token(perfil))).andExpect(status().isOk());
            mockMvc.perform(get("/gestion-tarjetas/cuentas/1/tarjetas").with(token(perfil))).andExpect(status().isOk());
            mockMvc.perform(get("/gestion-tarjetas/cuentas/1/movimientos").with(token(perfil))).andExpect(status().isOk());
            mockMvc.perform(get("/gestion-tarjetas/clientes/1/cuentas").with(token(perfil))).andExpect(status().isOk());
            mockMvc.perform(get("/gestion-tarjetas/tarjetas/1").with(token(perfil))).andExpect(status().isOk());
        }
        mockMvc.perform(get("/gestion-tarjetas/cuentas").with(tokenConPermisos(Permisos.CLIENTES_CONSULTAR, Permisos.SOLICITUDES_CONSULTAR)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crear solicitud sin empleador ni fecha de inicio laboral responde 400")
    void solicitudSinDatosLaborales() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/solicitudes")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":1,\"tipoTarjeta\":\"ORO\",\"ingresoMensual\":12000.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombreEmpleador").exists())
                .andExpect(jsonPath("$.errores.fechaInicioLaboral").exists());
        verifyNoInteractions(solicitudService);
    }

    @Test
    @DisplayName("Crear solicitud con fecha de inicio laboral futura responde 400")
    void solicitudConFechaInicioLaboralFutura() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/solicitudes")
                        .with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idCliente\":1,\"tipoTarjeta\":\"ORO\",\"ingresoMensual\":12000.00,"
                                + "\"nombreEmpleador\":\"Banco\",\"fechaInicioLaboral\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.fechaInicioLaboral").value("La fecha de inicio laboral no puede ser futura"));
        verifyNoInteractions(solicitudService);
    }

    @Test
    @DisplayName("CAJERO puede pagar pero no consumir ni reversar")
    void cajeroPagaPeroNoConsumeNiReversa() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/pagos").with(token(CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isCreated());
        verify(movimientoCuentaService).registrarPago(eq(1L), any(), any());

        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/consumos").with(token(CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isForbidden());
        for (String ruta : new String[] {"/gestion-tarjetas/consumos/1/reversar", "/gestion-tarjetas/pagos/1/reversar"}) {
            mockMvc.perform(put(ruta).with(token(CAJERO))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REVERSO))
                    .andExpect(status().isForbidden());
        }
        verify(movimientoCuentaService, never()).registrarConsumo(any(), any(), any());
        verify(movimientoCuentaService, never()).reversarConsumo(any(), any(), any());
        verify(movimientoCuentaService, never()).reversarPago(any(), any(), any());
    }

    @Test
    @DisplayName("OPERADOR registra consumos pero no pagos")
    void operadorConsumeNoPaga() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/consumos").with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/pagos").with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isForbidden());
        verify(movimientoCuentaService, never()).registrarPago(any(), any(), any());
    }

    @Test
    @DisplayName("ANALISTA_CREDITO decide solicitudes pero no crea ni modifica clientes ni registra solicitudes")
    void analistaDecidePeroNoGestionaClientes() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/aprobar").with(token(ANALISTA_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoAprobado\":1000.00}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/rechazar").with(token(ANALISTA_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comentarioDecision\":\"No cumple\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/gestion-tarjetas/solicitudes").with(token(ANALISTA_CREDITO))).andExpect(status().isOk());
        mockMvc.perform(get("/gestion-tarjetas/clientes/1/solicitudes").with(token(ANALISTA_CREDITO))).andExpect(status().isOk());

        String cliente = "{\"nombre\":\"Ana\"}";
        mockMvc.perform(post("/gestion-tarjetas/clientes").with(token(ANALISTA_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/gestion-tarjetas/clientes/1").with(token(ANALISTA_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON).content(cliente))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestion-tarjetas/solicitudes").with(token(ANALISTA_CREDITO))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("CAJERO no puede decidir solicitudes ni ver solicitudes, y no bloquea ni repone tarjetas")
    void cajeroSinPermisosDeSolicitudesNiTarjetas() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/solicitudes").with(token(CAJERO))).andExpect(status().isForbidden());
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/aprobar").with(token(CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoAprobado\":1000.00}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/bloquear").with(token(CAJERO))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivoBloqueo\":\"ROBO\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(solicitudService, tarjetaService);
    }

    @Test
    @DisplayName("OPERADOR gestiona clientes y registra solicitudes, pero no elimina clientes ni decide solicitudes")
    void operadorGestionaClientes() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/solicitudes").with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/rechazar").with(token(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"comentarioDecision\":\"No cumple\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/gestion-tarjetas/clientes/1").with(token(OPERADOR))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMINISTRADOR (todos los permisos) puede operar todos los endpoints sensibles")
    void administradorTieneAccesoTotal() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/solicitudes/1/aprobar").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montoAprobado\":1000.00}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/pagos").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/consumos").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_MOVIMIENTO))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/gestion-tarjetas/tarjetas/1/desbloquear").with(token(ADMINISTRADOR))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token sin ningún permiso recibe 403 en cualquier endpoint")
    void tokenSinPermisos() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/clientes").with(token(List.of()))).andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion-tarjetas/cuentas/1").with(token(List.of()))).andExpect(status().isForbidden());
        mockMvc.perform(get("/gestion-tarjetas/solicitudes").with(token(List.of()))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Solo con DATOS_SENSIBLES_VER la cuenta y la tarjeta traen los números completos; sin él, null")
    void datosSensiblesSegunPermiso() throws Exception {
        when(cuentaService.obtener(1L, true)).thenReturn(cuentaRespuesta(true));
        when(cuentaService.obtener(1L, false)).thenReturn(cuentaRespuesta(false));
        when(cuentaService.listarTodas(null, true)).thenReturn(List.of(cuentaRespuesta(true)));
        when(cuentaService.listarTodas(null, false)).thenReturn(List.of(cuentaRespuesta(false)));
        when(tarjetaService.obtener(1L, true)).thenReturn(tarjetaRespuesta(true));
        when(tarjetaService.obtener(1L, false)).thenReturn(tarjetaRespuesta(false));

        mockMvc.perform(get("/gestion-tarjetas/cuentas/1").with(token(ADMINISTRADOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroCuentaEnmascarado").value("**** **** **** 9015"))
                .andExpect(jsonPath("$.numeroCuentaCompleto").value("4200123456789015"))
                .andExpect(jsonPath("$.tarjetaVigente.numeroTarjetaCompleto").value("4200555123409875"))
                .andExpect(jsonPath("$.codigoCliente").value("10000001"));
        mockMvc.perform(get("/gestion-tarjetas/cuentas").with(token(ADMINISTRADOR)))
                .andExpect(jsonPath("$[0].numeroCuentaCompleto").value("4200123456789015"));
        mockMvc.perform(get("/gestion-tarjetas/tarjetas/1").with(token(ADMINISTRADOR)))
                .andExpect(jsonPath("$.numeroTarjetaCompleto").value("4200555123409875"));

        for (List<String> perfil : List.of(OPERADOR, CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(get("/gestion-tarjetas/cuentas/1").with(token(perfil)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.numeroCuentaEnmascarado").value("**** **** **** 9015"))
                    .andExpect(jsonPath("$.numeroCuentaCompleto").value(nullValue()))
                    .andExpect(jsonPath("$.tarjetaVigente.numeroTarjetaEnmascarado").value("**** **** **** 9875"))
                    .andExpect(jsonPath("$.tarjetaVigente.numeroCuentaCompleto").value(nullValue()))
                    .andExpect(jsonPath("$.tarjetaVigente.numeroTarjetaCompleto").value(nullValue()));
            mockMvc.perform(get("/gestion-tarjetas/cuentas").with(token(perfil)))
                    .andExpect(jsonPath("$[0].numeroCuentaCompleto").value(nullValue()));
            mockMvc.perform(get("/gestion-tarjetas/tarjetas/1").with(token(perfil)))
                    .andExpect(jsonPath("$.numeroCuentaCompleto").value(nullValue()))
                    .andExpect(jsonPath("$.numeroTarjetaCompleto").value(nullValue()));
        }
    }

    private CuentaRespuesta cuentaRespuesta(boolean conDatosSensibles) {
        return new CuentaRespuesta(3L, "**** **** **** 9015", conDatosSensibles ? "4200123456789015" : null,
                1L, "10000001", "Ana Morales", 10L, TipoTarjeta.ORO, "Oro", new BigDecimal("10000.00"),
                new BigDecimal("2500.00"), new BigDecimal("7500.00"), LocalDateTime.of(2026, 8, 1, 9, 0),
                EstadoPago.AL_DIA, "Al día", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 25),
                LocalDate.of(2026, 11, 5), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, tarjetaRespuesta(conDatosSensibles), 0,
                EstadoCuenta.ACTIVA, "Activa", null, null, null, null, null, false);
    }

    private TarjetaRespuesta tarjetaRespuesta(boolean conDatosSensibles) {
        return new TarjetaRespuesta(9L, 3L, "**** **** **** 9015", conDatosSensibles ? "4200123456789015" : null,
                "**** **** **** 9875", conDatosSensibles ? "4200555123409875" : null, TipoTarjeta.ORO, "Oro",
                "Ana Morales", "10000001", LocalDateTime.of(2026, 10, 9, 12, 0), LocalDate.of(2031, 10, 31),
                EstadoTarjeta.ACTIVA, "Activa", null, null, null, null, null, 5L, null, null, null, null, null,
                false, null, null, null, false, false);
    }

    private TarjetaRespuesta tarjetaNueva() {
        return tarjetaRespuesta(false);
    }

    @Test
    @DisplayName("OPERADOR y CAJERO reciben 403 al cancelar cuentas, registrar cargos y hacer ajustes de cargos")
    void sinPermisosDeCancelacionNiCargos() throws Exception {
        for (List<String> perfil : List.of(OPERADOR, CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(put("/gestion-tarjetas/cuentas/1/cancelar").with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_CANCELACION))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/gestion-tarjetas/cuentas/1/cargos").with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_CARGO))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put("/gestion-tarjetas/cargos/1/reversar").with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REVERSO))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/gestion-tarjetas/cuentas/1/exoneracion-mora").with(token(perfil))
                            .contentType(MediaType.APPLICATION_JSON).content(JSON_REVERSO))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(cargoService);
        verify(cuentaService, never()).cancelar(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("ADMINISTRADOR cancela cuentas, registra cargos (201), reversa cargos y exonera mora")
    void administradorCancelaYAjustaCargos() throws Exception {
        mockMvc.perform(put("/gestion-tarjetas/cuentas/1/cancelar").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_CANCELACION))
                .andExpect(status().isOk());
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/cargos").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_CARGO))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/gestion-tarjetas/cargos/1/reversar").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_REVERSO))
                .andExpect(status().isOk());
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/exoneracion-mora").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_REVERSO))
                .andExpect(status().isOk());
        verify(cargoService).registrar(eq(1L), any(), any());
        verify(cargoService).reversar(eq(1L), any(), any());
        verify(cargoService).exonerarMora(eq(1L), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Cargo sin tipo o con monto no positivo responde 400; la cancelación sin motivo también")
    void validacionesDeCargosYCancelacion() throws Exception {
        mockMvc.perform(post("/gestion-tarjetas/cuentas/1/cargos").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":0,\"descripcion\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoCargo").exists())
                .andExpect(jsonPath("$.errores.monto").exists());
        mockMvc.perform(put("/gestion-tarjetas/cuentas/1/cancelar").with(token(ADMINISTRADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.motivoCancelacion").exists());
        verifyNoInteractions(cargoService);
    }

    @Test
    @DisplayName("TARJETAS_RENOVAR: ADMINISTRADOR y OPERADOR renuevan (201); CAJERO y ANALISTA reciben 403")
    void renovarTarjetaSegunPermiso() throws Exception {
        when(tarjetaService.renovar(eq(1L), any(), anyBoolean())).thenReturn(tarjetaNueva());
        for (List<String> perfil : List.of(ADMINISTRADOR, OPERADOR)) {
            mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/renovaciones").with(token(perfil)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/gestion-tarjetas/tarjetas/9"))
                    .andExpect(jsonPath("$.id").value(9));
        }
        for (List<String> perfil : List.of(CAJERO, ANALISTA_CREDITO)) {
            mockMvc.perform(post("/gestion-tarjetas/tarjetas/1/renovaciones").with(token(perfil)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("El filtro ?estado= de las cuentas se pasa al servicio y un valor inválido responde 400")
    void filtroDeCuentasPorEstado() throws Exception {
        mockMvc.perform(get("/gestion-tarjetas/cuentas?estado=CANCELADA").with(token(OPERADOR))).andExpect(status().isOk());
        verify(cuentaService).listarTodas(eq(EstadoCuenta.CANCELADA), anyBoolean());
        mockMvc.perform(get("/gestion-tarjetas/cuentas?estado=OTRO").with(token(OPERADOR))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Preflight CORS desde el frontend permitido responde 200 con Access-Control-Allow-Origin")
    void preflightCorsDesdeOrigenPermitido() throws Exception {
        mockMvc.perform(options("/gestion-tarjetas/clientes")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    @Test
    @DisplayName("Preflight CORS desde un origen no permitido responde 403")
    void preflightCorsDesdeOrigenNoPermitido() throws Exception {
        mockMvc.perform(options("/gestion-tarjetas/clientes")
                        .header(HttpHeaders.ORIGIN, "http://sitio-externo.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
