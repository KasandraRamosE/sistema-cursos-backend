package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InscripcionDto {
    private Long idInscripcion;
    private Long idParticipante;
    private String nombreParticipante;
    private String emailParticipante;
    private String usernameParticipante;

    // Info de la actividad
    private Long idCurso;
    private Long idEvento;
    private String nombreActividad;  // nombre del curso o evento
    private String codigoParalelo;
    private String tipoActividad;    // "CURSO" o "EVENTO"
    private Long idCarrera;
    private String nombreCarrera;
    private String modalidadActividad;

    private String tipoPrecio;
    private BigDecimal saldo;
    private String estado;
    private LocalDateTime fechaInscripcion;

    // Info del pago si existe
    private String estadoPago;
    private String referenciaTransaccion;
}