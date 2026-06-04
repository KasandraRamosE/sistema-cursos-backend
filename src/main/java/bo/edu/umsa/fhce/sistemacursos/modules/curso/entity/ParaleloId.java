package bo.edu.umsa.fhce.sistemacursos.modules.curso.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

// PK compuesta de Paralelo: (id_curso, codigo)
// Paralelo es entidad débil — su identidad depende del Curso padre
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParaleloId implements Serializable {

    @Column(name = "id_curso")
    private Long idCurso;

    // "A", "B", "01", etc.
    @Column(name = "codigo", length = 10)
    private String codigo;
}