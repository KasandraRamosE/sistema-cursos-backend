package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlantillaEstadoResumenDto {
    private Long idCurso;
    private Long idEvento;
    private String estado;
    private Integer version;
}
