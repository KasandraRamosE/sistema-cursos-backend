package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PagoDto {
    private Long idPago;
    private Long idInscripcion;
    private BigDecimal monto;
    private String metodoPago;
    private String referenciaTransaccion;
    private String estado;
    private LocalDateTime fechaPago;
}