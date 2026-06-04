package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActualizarParalelosRequest {

    @NotNull(message = "La lista de paralelos es obligatoria")
    private List<ParaleloRefRequest> paralelos;
}
