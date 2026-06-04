package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistrarNotaRequest {

    @NotNull(message = "El id de inscripción es obligatorio")
    private Long idInscripcion;

    @NotNull(message = "La nota final es obligatoria")
    @DecimalMin(value = "0.00", message = "La nota no puede ser negativa")
    @DecimalMax(value = "100.00", message = "La nota no puede superar 100")
    private java.math.BigDecimal notaFinal;
}