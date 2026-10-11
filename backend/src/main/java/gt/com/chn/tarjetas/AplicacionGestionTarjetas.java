package gt.com.chn.tarjetas;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AplicacionGestionTarjetas {
    public static void main(String[] argumentos) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Guatemala"));
        SpringApplication.run(AplicacionGestionTarjetas.class, argumentos);
    }
}
