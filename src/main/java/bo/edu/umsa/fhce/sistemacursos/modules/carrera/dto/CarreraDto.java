package bo.edu.umsa.fhce.sistemacursos.modules.carrera.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CarreraDto {
    private Long idCarrera;
    private String nombre;
    private String estado;
}