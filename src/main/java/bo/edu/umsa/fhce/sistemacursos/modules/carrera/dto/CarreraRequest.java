package bo.edu.umsa.fhce.sistemacursos.modules.carrera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CarreraRequest {

    @NotBlank(message = "El nombre de la carrera es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;
}