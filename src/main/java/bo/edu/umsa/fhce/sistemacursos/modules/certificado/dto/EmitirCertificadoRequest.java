package bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmitirCertificadoRequest {

    @NotNull(message = "El id de inscripción es obligatorio")
    private Long idInscripcion;
}