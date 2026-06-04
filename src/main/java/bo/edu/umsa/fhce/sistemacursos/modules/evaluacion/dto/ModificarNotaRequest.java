// src/main/java/.../modules/evaluacion/dto/ModificarNotaRequest.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ModificarNotaRequest {

    @NotNull(message = "La nota nueva es obligatoria")
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private java.math.BigDecimal notaNueva;

    @NotBlank(message = "El motivo del cambio es obligatorio")
    private String motivo;
}