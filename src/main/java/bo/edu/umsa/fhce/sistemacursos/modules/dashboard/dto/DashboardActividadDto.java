package bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardActividadDto {
    private String id;
    private String nombre;
    private String tipo;
    private String carrera;
    private Integer inscritos;
    private Integer cupo;
    private String estado;
}
