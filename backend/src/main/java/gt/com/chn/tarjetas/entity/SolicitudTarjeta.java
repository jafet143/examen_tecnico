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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitud_tarjeta", schema = "dbo")
@NoArgsConstructor
@Getter
public class SolicitudTarjeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    @Setter
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_tarjeta", nullable = false, length = 10)
    @Setter
    private TipoTarjeta tipoTarjeta;

    @Column(name = "fecha_solicitud", nullable = false)
    @Setter
    private LocalDateTime fechaSolicitud;

    @Column(name = "ingreso_mensual", nullable = false, precision = 18, scale = 2)
    @Setter
    private BigDecimal ingresoMensual;

    @Column(name = "nombre_empleador", nullable = false, length = 150)
    @Setter
    private String nombreEmpleador;

    @Column(name = "fecha_inicio_laboral", nullable = false)
    @Setter
    private LocalDate fechaInicioLaboral;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    @Setter
    private EstadoSolicitud estado;

    @Column(name = "fecha_decision")
    @Setter
    private LocalDateTime fechaDecision;

    @Column(name = "comentario_decision", length = 500)
    @Setter
    private String comentarioDecision;

    @Column(name = "monto_aprobado", precision = 18, scale = 2)
    @Setter
    private BigDecimal montoAprobado;

    @Column(name = "usuario_decision", length = 100)
    @Setter
    private String usuarioDecision;

    @OneToOne(mappedBy = "solicitud", fetch = FetchType.LAZY)
    private CuentaCredito cuenta;

    @PrePersist
    void asignarValoresIniciales() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoSolicitud.PENDIENTE;
        }
    }

    public static int calcularMesesAntiguedad(LocalDate inicioLaboral, LocalDate fechaReferencia) {
        return (int) ChronoUnit.MONTHS.between(inicioLaboral, fechaReferencia);
    }

    public int obtenerMesesAntiguedadLaboral() {
        return calcularMesesAntiguedad(fechaInicioLaboral, fechaSolicitud.toLocalDate());
    }
}
