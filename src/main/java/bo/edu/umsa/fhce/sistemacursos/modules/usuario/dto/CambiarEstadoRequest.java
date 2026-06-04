package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CambiarEstadoRequest {

    @NotBlank(message = "El estado es obligatorio")
    // Valores: ACTIVO, INACTIVO
    private String estado;
}