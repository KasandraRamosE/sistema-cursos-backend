package bo.edu.umsa.fhce.sistemacursos.modules.curso.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paralelo")
@Getter @Setter
@NoArgsConstructor
public class Paralelo {

    @EmbeddedId
    private ParaleloId id;

    // MapsId("idCurso"): la parte idCurso de la PK viene de esta relación
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idCurso")
    @JoinColumn(name = "id_curso")
    private Curso curso;

    // El docente puede ser null — se asigna después
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_docente")
    private Usuario docente;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 10)
    private Modalidad modalidad;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Column(name = "horario_descripcion", length = 255)
    private String horarioDescripcion;

    @Column(name = "lugar", length = 255)
    private String lugar;

    @Column(name = "link", length = 255)
    private String link;

    // Constructor de conveniencia para crear un paralelo con su curso
    public Paralelo(Curso curso, String codigo) {
        this.curso = curso;
        this.id    = new ParaleloId(curso.getIdCurso(), codigo);
    }

    public enum Modalidad {
        PRESENCIAL, VIRTUAL, MIXTO
    }
}