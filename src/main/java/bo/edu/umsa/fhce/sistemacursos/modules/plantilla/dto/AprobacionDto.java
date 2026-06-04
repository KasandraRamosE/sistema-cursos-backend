package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AprobacionDto {
    private Long idAprobacion;
    private String estado;
    private String observaciones;
    private LocalDateTime fechaRevision;
    private String coordinador;
}
