package bo.edu.umsa.fhce.sistemacursos.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerificarCodigoResetRequest {
    @NotBlank(message = "El username es requerido")
    private String username;

    @NotBlank(message = "El código es requerido")
    private String codigo;
}
