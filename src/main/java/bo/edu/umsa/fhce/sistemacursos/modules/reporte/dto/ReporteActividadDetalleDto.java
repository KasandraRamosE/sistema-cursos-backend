package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReporteActividadDetalleDto {
    private String tipo;
    private Long idActividad;
    private String nombre;
    private String carrera;
    private String estado;
    private Long inscritosConfirmados;
    private Long cupoMaximo;
    private Long cuposDisponibles;
    private Long participantesUmsa;
    private Long participantesExterno;
    private Long aprobados;
    private Long asistidos;
    private BigDecimal ingresosUmsa;
    private BigDecimal ingresosExterno;
    private BigDecimal ingresosTotal;
}
