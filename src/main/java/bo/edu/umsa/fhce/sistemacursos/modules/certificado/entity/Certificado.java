package bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificado")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificado")
    private Long idCertificado;

    // Varias versiones pueden pertenecer a la misma inscripción.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    // Código único para el QR — UUID generado en el servicio
    @Column(name = "codigo_verificacion", nullable = false, unique = true, length = 100)
    private String codigoVerificacion;

    // Ruta del archivo PDF generado en el servidor
    @Column(name = "archivo_generado", length = 255)
    private String archivoGenerado;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_emision", nullable = false, length = 10)
    private EstadoEmision estadoEmision = EstadoEmision.GENERADO;

    // Incrementa con cada reemisión: 1, 2, 3...
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @PrePersist
    protected void onCreate() {
        this.fechaEmision = LocalDateTime.now();
        if (this.estadoEmision == null) this.estadoEmision = EstadoEmision.GENERADO;
        if (this.version == null) this.version = 1;
    }

    public enum EstadoEmision {
        GENERADO, ANULADO, REEMITIDO
    }
}
