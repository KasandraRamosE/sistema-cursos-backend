package bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class CertificadoDto {
    private Long idCertificado;
    private Long idInscripcion;
    private String nombreParticipante;
    private String nombreActividad;
    private String tipoActividad;      // CURSO o EVENTO
    private Integer cargaHoraria;
    private String notaFinal;          // Solo para cursos, null para eventos
    private String estadoEmision;
    private Integer version;
    private String codigoVerificacion;
    private String urlVerificacion;    // URL completa para el QR
    private LocalDateTime fechaEmision;
}