package gt.com.chn.tarjetas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pago", schema = "dbo")
@NoArgsConstructor
@Getter
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    @Setter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuenta", nullable = false)
    @Setter
    private CuentaCredito cuenta;

    @Column(name = "monto", nullable = false, precision = 18, scale = 2)
    @Setter
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    @Setter
    private LocalDateTime fecha;

    @Column(name = "descripcion", nullable = false, length = 250)
    @Setter
    private String descripcion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "usuario_registro", length = 100)
    @Setter
    private String usuarioRegistro;

    @Column(name = "reversado", nullable = false)
    @Getter(AccessLevel.NONE)
    private boolean reversado;

    @Column(name = "motivo_reverso", length = 250)
    private String motivoReverso;

    @Column(name = "fecha_reverso")
    private LocalDateTime fechaReverso;

    @Column(name = "usuario_reverso", length = 100)
    private String usuarioReverso;

    @PrePersist
    void asignarFechaRegistro() {
        this.fechaRegistro = LocalDateTime.now();
    }

    public void reversar(String motivo, LocalDateTime fecha, String usuario) {
        this.reversado = true;
        this.motivoReverso = motivo;
        this.fechaReverso = fecha;
        this.usuarioReverso = usuario;
    }

    public boolean estaReversado() {
        return reversado;
    }
}
