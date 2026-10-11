package gt.com.chn.tarjetas.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionReloj {

    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Guatemala"));
    }
}
