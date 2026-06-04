package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Cada revisión del coordinador genera un nuevo registro
// Una plantilla puede tener múltiples revisiones (si se rechaza y se vuelve a revisar)
@Entity
@Table(name = "aprobacion")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aprobacion")
    private Long idAprobacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla", nullable = false)
    private PlantillaCertificado plantilla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_coordinador", nullable = false)
    private Usuario coordinador;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoAprobacion estado;

    // Feedback del coordinador al diseñador
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_revision", nullable = false, updatable = false)
    private LocalDateTime fechaRevision;

    @PrePersist
    protected void onCreate() {
        this.fechaRevision = LocalDateTime.now();
    }

    public enum EstadoAprobacion {
        APROBADA, RECHAZADA
    }
}