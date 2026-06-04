// src/main/java/.../modules/evaluacion/entity/SolicitudEmision.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Generada cuando el docente confirma notas
// Flujo: PENDIENTE → EN_PROCESO → COMPLETADO
@Entity
@Table(name = "solicitud_emision")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudEmision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long idSolicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @Column(name = "codigo_paralelo", length = 10)
    private String codigoParalelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente", nullable = true)
    private Usuario docente;

    @Column(name = "cantidad_aprobados", nullable = false)
    private Integer cantidadAprobados;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procesado_por")
    private Usuario procesadoPor;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @PrePersist
    protected void onCreate() {
        this.fechaSolicitud = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSolicitud.PENDIENTE;
        }
    }

    public enum EstadoSolicitud {
        PENDIENTE, EN_PROCESO, COMPLETADO
    }
}