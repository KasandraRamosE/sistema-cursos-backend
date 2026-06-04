package bo.edu.umsa.fhce.sistemacursos.modules.curso.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
public class CursoDto {
    private Long idCurso;
    private Long idCarrera;
    private String nombreCarrera;
    private String nombreOrganizador;
    private Long idDisenador;
    private String nombreDisenador;
    private String nombre;
    private String descripcion;
    private String imagen;
    private Integer cargaHoraria;
    private Integer duracion;
    private String unidad;
    private LocalDate fechaInicio;
    private BigDecimal costoExterno;
    private BigDecimal costoUmsa;
    private BigDecimal notaAprobacion;
    private String estado;
    private LocalDateTime fechaCreacion;
    private List<ParaleloDto> paralelos;
}