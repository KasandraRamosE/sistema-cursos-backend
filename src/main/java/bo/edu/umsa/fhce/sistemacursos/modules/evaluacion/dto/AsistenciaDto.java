package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class AsistenciaDto {
    private Long idAsistencia;
    private Long idInscripcion;
    private String nombreParticipante;
    private String registradoPor;
    private LocalDateTime fechaRegistro;
}