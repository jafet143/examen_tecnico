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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cuenta_credito", schema = "dbo")
@NoArgsConstructor
@Getter
public class CuentaCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta")
    @Setter
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "numero_cuenta", nullable = false, length = 16)
    @Setter
    private String numeroCuenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    @Setter
    private Cliente cliente;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_solicitud", nullable = false, unique = true)
    @Setter
    private SolicitudTarjeta solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarjeta", nullable = false, length = 10)
    @Setter
    private TipoTarjeta tipoTarjeta;

    @Column(name = "limite_credito", nullable = false, precision = 12, scale = 2)
    @Setter
    private BigDecimal limiteCredito;

    @Column(name = "saldo_actual", nullable = false, precision = 12, scale = 2)
    @Setter
    private BigDecimal saldoActual;

    @Column(name = "fecha_apertura", nullable = false)
    @Setter
    private LocalDateTime fechaApertura;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cuenta", nullable = false, length = 10)
    private EstadoCuenta estadoCuenta = EstadoCuenta.ACTIVA;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cancelacion", length = 20)
    private MotivoCancelacion motivoCancelacion;

    @Column(name = "comentario_cancelacion", length = 250)
    private String comentarioCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "usuario_cancelacion", length = 100)
    private String usuarioCancelacion;

    public BigDecimal obtenerCreditoDisponible() {
        return limiteCredito.subtract(saldoActual).max(BigDecimal.ZERO);
    }

    public boolean estaCancelada() {
        return estadoCuenta == EstadoCuenta.CANCELADA;
    }

    public void cancelar(MotivoCancelacion motivo, String comentario, LocalDateTime fecha, String usuario) {
        this.estadoCuenta = EstadoCuenta.CANCELADA;
        this.motivoCancelacion = motivo;
        this.comentarioCancelacion = comentario;
        this.fechaCancelacion = fecha;
        this.usuarioCancelacion = usuario;
    }
}
