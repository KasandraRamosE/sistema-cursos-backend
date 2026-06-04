package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class AsistenciaAdminDto {
    private Long idInscripcion;
    private Long idAsistencia;
    private String nombreParticipante;
    private String username;
    private String email;
    private boolean asistio;
    private String registradoPor;
    private LocalDateTime fechaRegistro;
    private String estadoInscripcion;
}
