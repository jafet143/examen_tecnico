package gt.com.chn.tarjetas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tarjeta_credito", schema = "dbo")
@NoArgsConstructor
@Getter
public class TarjetaCredito {
    private static final int ANIOS_VIGENCIA = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarjeta")
    @Setter
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "numero_tarjeta", nullable = false, length = 16)
    @Setter
    private String numeroTarjeta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuenta", nullable = false)
    @Setter
    private CuentaCredito cuenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_tarjeta", nullable = false, length = 12)
    private EstadoTarjeta estadoTarjeta = EstadoTarjeta.ACTIVA;

    @Column(name = "fecha_emision", nullable = false)
    @Setter
    private LocalDateTime fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    @Setter
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_bloqueo", length = 20)
    private MotivoBloqueo motivoBloqueo;

    @Column(name = "comentario_bloqueo", length = 250)
    private String comentarioBloqueo;

    @Column(name = "fecha_bloqueo")
    private LocalDateTime fechaBloqueo;

    @Column(name = "usuario_bloqueo", length = 100)
    private String usuarioBloqueo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tarjeta_anterior")
    @Setter
    private TarjetaCredito tarjetaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_reposicion", length = 20)
    private MotivoReposicion motivoReposicion;

    @Column(name = "comentario_reposicion", length = 250)
    private String comentarioReposicion;

    @Column(name = "fecha_reposicion")
    private LocalDateTime fechaReposicion;

    @Column(name = "usuario_reposicion", length = 100)
    private String usuarioReposicion;

    @Column(name = "denuncia_presentada")
    private Boolean denunciaPresentada;

    @Column(name = "tarjeta_deteriorada_presentada")
    private Boolean tarjetaDeterioradaPresentada;

    @Column(name = "id_cargo_reposicion")
    @Setter
    private Long idCargoReposicion;

    public static LocalDate calcularFechaVencimiento(LocalDateTime emision) {
        return emision.toLocalDate().plusYears(ANIOS_VIGENCIA).with(TemporalAdjusters.lastDayOfMonth());
    }

    public boolean estaBloqueada() {
        return estadoTarjeta == EstadoTarjeta.BLOQUEADA;
    }

    public boolean estaReemplazada() {
        return estadoTarjeta == EstadoTarjeta.REEMPLAZADA;
    }

    public boolean estaCancelada() {
        return estadoTarjeta == EstadoTarjeta.CANCELADA;
    }

    public boolean estaVencida(LocalDate hoy) {
        return !estaReemplazada() && !estaCancelada() && hoy.isAfter(fechaVencimiento);
    }

    public boolean puedeRenovarse(LocalDate hoy) {
        return estadoTarjeta == EstadoTarjeta.ACTIVA && estaVencida(hoy);
    }

    public boolean puedeDesbloquearse() {
        return estaBloqueada() && motivoBloqueo.permiteDesbloqueo();
    }

    public void bloquear(MotivoBloqueo motivo, String comentario, LocalDateTime fecha, String usuario) {
        this.estadoTarjeta = EstadoTarjeta.BLOQUEADA;
        this.motivoBloqueo = motivo;
        this.comentarioBloqueo = comentario;
        this.fechaBloqueo = fecha;
        this.usuarioBloqueo = usuario;
    }

    public void desbloquear() {
        this.estadoTarjeta = EstadoTarjeta.ACTIVA;
        this.motivoBloqueo = null;
        this.comentarioBloqueo = null;
        this.fechaBloqueo = null;
        this.usuarioBloqueo = null;
    }

    public void reemplazar(MotivoReposicion motivo, String comentario, LocalDateTime fecha, String usuario,
                           Boolean denunciaPresentada, Boolean tarjetaDeterioradaPresentada) {
        this.estadoTarjeta = EstadoTarjeta.REEMPLAZADA;
        this.motivoReposicion = motivo;
        this.comentarioReposicion = comentario;
        this.fechaReposicion = fecha;
        this.usuarioReposicion = usuario;
        this.denunciaPresentada = denunciaPresentada;
        this.tarjetaDeterioradaPresentada = tarjetaDeterioradaPresentada;
    }

    public void cancelar() {
        this.estadoTarjeta = EstadoTarjeta.CANCELADA;
    }
}
