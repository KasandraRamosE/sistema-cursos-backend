package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReporteFinancieroActividadDto {
    private String tipo;
    private Long idActividad;
    private String nombre;
    private String carrera;
    private BigDecimal ingresosUmsa;
    private BigDecimal ingresosExterno;
    private BigDecimal ingresosTotal;

    public ReporteFinancieroActividadDto(
            String tipo,
            Long idActividad,
            String nombre,
            String carrera,
            BigDecimal ingresosUmsa,
            BigDecimal ingresosExterno) {
        this.tipo = tipo;
        this.idActividad = idActividad;
        this.nombre = nombre;
        this.carrera = carrera;
        this.ingresosUmsa = ingresosUmsa != null ? ingresosUmsa : BigDecimal.ZERO;
        this.ingresosExterno = ingresosExterno != null ? ingresosExterno : BigDecimal.ZERO;
        this.ingresosTotal = this.ingresosUmsa.add(this.ingresosExterno);
    }
}
