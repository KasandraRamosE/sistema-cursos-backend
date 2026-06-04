// src/main/java/.../modules/carrera/entity/CoordinadorCarrera.java

package bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity;

import java.time.LocalDateTime;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coordinador_carrera")
@Getter @Setter
@NoArgsConstructor
public class CoordinadorCarrera {

    @EmbeddedId
    private CoordinadorCarreraId id;

    // MapsId indica qué parte de la PK compuesta viene de esta relación
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCoordinador")
    @JoinColumn(name = "id_coordinador")
    private Usuario coordinador;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCarrera")
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @PrePersist
    protected void onCreate() {
        this.fechaAsignacion = LocalDateTime.now();
    }

    // Constructor de conveniencia — inicializa la PK compuesta automáticamente
    public CoordinadorCarrera(Usuario coordinador, Carrera carrera) {
        this.coordinador = coordinador;
        this.carrera     = carrera;
        this.id          = new CoordinadorCarreraId(
            coordinador.getIdUsuario(),
            carrera.getIdCarrera()
        );
    }
}