package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AprobacionRequest {

    @NotBlank(message = "El estado es obligatorio")
    // Valores: APROBADA, RECHAZADA
    private String estado;

    // Obligatorio si se rechaza — feedback para el diseñador
    private String observaciones;
}