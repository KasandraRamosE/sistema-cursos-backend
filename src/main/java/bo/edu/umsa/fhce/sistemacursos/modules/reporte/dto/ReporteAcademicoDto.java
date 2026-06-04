package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReporteAcademicoDto {
    private List<ReporteAcademicoCursoDto> cursos;
    private List<ReporteAcademicoEventoDto> eventos;
}
