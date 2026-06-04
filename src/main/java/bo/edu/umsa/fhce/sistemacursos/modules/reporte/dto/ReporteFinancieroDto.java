package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteFinancieroDto {
    private List<ReporteFinancieroActividadDto> cursos;
    private List<ReporteFinancieroActividadDto> eventos;
    private BigDecimal totalUmsa;
    private BigDecimal totalExterno;
    private BigDecimal totalGeneral;
}
