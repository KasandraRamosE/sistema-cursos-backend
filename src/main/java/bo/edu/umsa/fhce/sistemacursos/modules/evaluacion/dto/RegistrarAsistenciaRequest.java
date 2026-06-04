package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistrarAsistenciaRequest {

    @NotNull(message = "El id de inscripción es obligatorio")
    private Long idInscripcion;
}