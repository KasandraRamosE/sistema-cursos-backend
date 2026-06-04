package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.time.LocalDate;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAcademicoCursoDto {
    private Long idCurso;
    private String nombre;
    private String carrera;
    private LocalDate fechaInicio;
    private Long cupoMaximo;
    private Curso.EstadoCurso estado;
    private Long inscritosConfirmados;
}
