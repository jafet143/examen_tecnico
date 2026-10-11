package gt.com.chn.tarjetas.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public boolean puedeVerDatosSensibles() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> autoridad.getAuthority()
                        .equals(ConvertidorRolesKeycloak.PREFIJO_ROL + Permisos.DATOS_SENSIBLES_VER));
    }
}
