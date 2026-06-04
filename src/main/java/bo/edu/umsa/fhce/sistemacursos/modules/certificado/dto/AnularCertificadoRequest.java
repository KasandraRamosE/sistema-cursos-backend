package bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AnularCertificadoRequest {

    @NotBlank(message = "El motivo de anulación es obligatorio")
    private String motivo;

    // Si es true, genera un nuevo certificado al anular
    private boolean reemitir = false;
}