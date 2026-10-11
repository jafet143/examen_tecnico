package gt.com.chn.tarjetas.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class ConvertidorRolesKeycloak implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String PREFIJO_ROL = "ROLE_";

    private static final String CLAIM_ACCESO_REALM = "realm_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_NOMBRE_USUARIO = "preferred_username";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> autoridades = extraerRoles(jwt).stream()
                .<GrantedAuthority>map(rol -> new SimpleGrantedAuthority(PREFIJO_ROL + rol))
                .toList();
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getClaimAsString(CLAIM_NOMBRE_USUARIO));
    }

    private List<String> extraerRoles(Jwt jwt) {
        Map<String, Object> accesoRealm = jwt.getClaimAsMap(CLAIM_ACCESO_REALM);
        if (accesoRealm == null || !(accesoRealm.get(CLAIM_ROLES) instanceof Collection<?> roles)) {
            return Collections.emptyList();
        }
        return roles.stream().map(Object::toString).toList();
    }
}
