package bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class VerificacionDto {
    private String estado;             // VÁLIDO, ANULADO, REEMITIDO
    private String nombreTitular;
    private String nombreActividad;
    private Integer cargaHoraria;
    private String notaFinal;
    private LocalDateTime fechaEmision;
    private Integer version;
    private String urlCertificadoReemplazo; // solo si fue reemitido
}