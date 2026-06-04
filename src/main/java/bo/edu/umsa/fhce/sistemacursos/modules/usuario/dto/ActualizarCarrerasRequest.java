package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarCarrerasRequest {

    @NotNull(message = "La lista de carreras es obligatoria")
    @NotEmpty(message = "Debe incluir al menos una carrera")
    private List<Long> carreraIds;
}
