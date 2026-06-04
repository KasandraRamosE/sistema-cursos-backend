package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ConfirmarNotasRequest {

    // Código del paralelo cuyas notas se confirman
    @NotBlank(message = "El código del paralelo es obligatorio")
    private String codigoParalelo;

    // Observaciones opcionales del docente
    private String notas;
}