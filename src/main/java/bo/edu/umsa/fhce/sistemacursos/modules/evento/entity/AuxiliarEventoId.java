package bo.edu.umsa.fhce.sistemacursos.modules.evento.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AuxiliarEventoId implements Serializable {

    @Column(name = "id_auxiliar")
    private Long idAuxiliar;

    @Column(name = "id_evento")
    private Long idEvento;
}