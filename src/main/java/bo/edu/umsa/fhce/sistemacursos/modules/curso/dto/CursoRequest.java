package bo.edu.umsa.fhce.sistemacursos.modules.curso.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CursoRequest {

    @NotNull(message = "La carrera es obligatoria")
    private Long idCarrera;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    private String nombre;

    private String descripcion;

    @Size(max = 255, message = "La imagen no puede superar 255 caracteres")
    private String imagen;

    @NotNull(message = "La carga horaria es obligatoria")
    @Positive(message = "La carga horaria debe ser mayor a 0")
    private Integer cargaHoraria;

    @Pattern(regexp = "^(días|semanas|meses)$", message = "La unidad debe ser días, semanas o meses")
    private String unidad;

    private Integer duracion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @FutureOrPresent(message = "La fecha de inicio no puede ser en el pasado")
    private LocalDate fechaInicio;

    @NotNull(message = "El costo externo es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo externo no puede ser negativo")
    private BigDecimal costoExterno;

    @NotNull(message = "El costo UMSA es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo UMSA no puede ser negativo")
    private BigDecimal costoUmsa;

    @NotNull(message = "La nota de aprobación es obligatoria")
    @DecimalMin(value = "0.00", message = "La nota mínima no puede ser negativa")
    @DecimalMax(value = "100.00", message = "La nota mínima no puede superar 100")
    private BigDecimal notaAprobacion;

    public Long getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(Long idCarrera) {
        this.idCarrera = idCarrera;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public Integer getCargaHoraria() {
        return cargaHoraria;
    }

    public void setCargaHoraria(Integer cargaHoraria) {
        this.cargaHoraria = cargaHoraria;
    }

    public String getUnidad() {
        return unidad;
    }

    public void setUnidad(String unidad) {
        this.unidad = unidad;
    }

    public Integer getDuracion() {
        return duracion;
    }

    public void setDuracion(Integer duracion) {
        this.duracion = duracion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public BigDecimal getCostoExterno() {
        return costoExterno;
    }

    public void setCostoExterno(BigDecimal costoExterno) {
        this.costoExterno = costoExterno;
    }

    public BigDecimal getCostoUmsa() {
        return costoUmsa;
    }

    public void setCostoUmsa(BigDecimal costoUmsa) {
        this.costoUmsa = costoUmsa;
    }

    public BigDecimal getNotaAprobacion() {
        return notaAprobacion;
    }

    public void setNotaAprobacion(BigDecimal notaAprobacion) {
        this.notaAprobacion = notaAprobacion;
    }
}