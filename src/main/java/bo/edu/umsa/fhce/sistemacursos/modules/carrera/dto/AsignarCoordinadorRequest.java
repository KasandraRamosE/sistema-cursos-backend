package bo.edu.umsa.fhce.sistemacursos.modules.carrera.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AsignarCoordinadorRequest {

    @NotNull(message = "El id del coordinador es obligatorio")
    private Long idCoordinador;
}