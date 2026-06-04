package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class EvaluacionDto {
    private Long idEvaluacion;
    private Long idInscripcion;
    private String nombreParticipante;
    private String username;
    private BigDecimal notaFinal;
    private String estado;           // APROBADO o REPROBADO
    private LocalDateTime fechaRegistro;
}