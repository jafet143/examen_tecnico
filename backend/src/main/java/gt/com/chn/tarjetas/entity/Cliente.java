package gt.com.chn.tarjetas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cliente", schema = "dbo")
@NoArgsConstructor
@Getter
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    @Setter
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo_cliente", nullable = false, updatable = false, length = 8)
    @Setter
    private String codigoCliente;

    @Column(name = "nombre", nullable = false, length = 100)
    @Setter
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    @Setter
    private String apellido;

    @Column(name = "numero_identificacion", nullable = false, length = 20)
    @Setter
    private String numeroIdentificacion;

    @Column(name = "fecha_nacimiento", nullable = false)
    @Setter
    private LocalDate fechaNacimiento;

    @Column(name = "direccion", nullable = false, length = 250)
    @Setter
    private String direccion;

    @Column(name = "correo_electronico", nullable = false, length = 150)
    @Setter
    private String correoElectronico;

    @Column(name = "telefono", nullable = false, length = 20)
    @Setter
    private String telefono;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void asignarFechaCreacion() {
        this.fechaCreacion = LocalDateTime.now();
    }

    @PreUpdate
    void asignarFechaActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
