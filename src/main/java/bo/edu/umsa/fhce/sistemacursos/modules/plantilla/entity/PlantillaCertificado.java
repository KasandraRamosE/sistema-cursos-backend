package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "plantilla_certificado")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaCertificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long idPlantilla;

    // Solo uno tiene valor — el otro es null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    // Ruta del archivo PDF guardado en el servidor
    @Column(name = "archivo_pdf", nullable = false, length = 255)
    private String archivoPdf;

    // Incrementa con cada nueva versión subida
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subida_por", nullable = false)
    private Usuario subidaPor;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoPlantilla estado = EstadoPlantilla.PENDIENTE;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoPlantilla.PENDIENTE;
        if (this.version == null) this.version = 1;
    }

    public enum EstadoPlantilla {
        PENDIENTE, VIGENTE, HISTORICA
    }
    
}