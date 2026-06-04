package bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Clave compuesta para CoordinadorCarrera
// JPA requiere que las PK compuestas implementen Serializable
@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode  // Lombok genera equals y hashCode — obligatorio para PKs compuestas
public class CoordinadorCarreraId implements Serializable {

    @Column(name = "id_coordinador")
    private Long idCoordinador;

    @Column(name = "id_carrera")
    private Long idCarrera;
}