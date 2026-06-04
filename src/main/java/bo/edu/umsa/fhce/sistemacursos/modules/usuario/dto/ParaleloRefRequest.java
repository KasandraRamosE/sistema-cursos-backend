package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParaleloRefRequest {

    @NotNull(message = "El id del curso es obligatorio")
    private Long idCurso;

    @NotBlank(message = "El codigo del paralelo es obligatorio")
    private String codigo;
}
