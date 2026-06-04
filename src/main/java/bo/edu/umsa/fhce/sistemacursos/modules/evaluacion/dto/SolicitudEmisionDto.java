package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class SolicitudEmisionDto {
    private Long idSolicitud;
    private String nombreActividad;
    private String codigoParalelo;
    private String nombreDocente;
    private Integer cantidadAprobados;
    private String estado;
    private String notas;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaProcesamiento;
}