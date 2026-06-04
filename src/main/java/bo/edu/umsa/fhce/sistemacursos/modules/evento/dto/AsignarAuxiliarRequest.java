package bo.edu.umsa.fhce.sistemacursos.modules.evento.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AsignarAuxiliarRequest {

    @NotNull(message = "El id del auxiliar es obligatorio")
    private Long idAuxiliar;
}