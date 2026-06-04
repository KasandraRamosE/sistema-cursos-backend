package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteParticipacionDto {
    private List<ReporteParticipacionCarreraDto> carreras;
    private long totalUmsa;
    private long totalExterno;
    private long totalGeneral;
}
