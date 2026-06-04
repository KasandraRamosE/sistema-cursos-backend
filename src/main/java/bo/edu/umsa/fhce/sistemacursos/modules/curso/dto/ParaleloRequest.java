package bo.edu.umsa.fhce.sistemacursos.modules.curso.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ParaleloRequest {

    @NotBlank(message = "El código del paralelo es obligatorio")
    @Size(max = 10, message = "El código no puede superar 10 caracteres")
    private String codigo;

    // Puede ser null — docente se asigna después
    private Long idDocente;

    @NotNull(message = "La modalidad es obligatoria")
    private String modalidad; // PRESENCIAL, VIRTUAL, MIXTO

    @Positive(message = "El cupo debe ser mayor a 0")
    private Integer cupoMaximo; // null = sin límite

    @Size(max = 255)
    private String horarioDescripcion;

    @Size(max = 255)
    private String lugar;

    @Size(max = 255)
    private String link;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Long getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(Long idDocente) {
        this.idDocente = idDocente;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public String getHorarioDescripcion() {
        return horarioDescripcion;
    }

    public void setHorarioDescripcion(String horarioDescripcion) {
        this.horarioDescripcion = horarioDescripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public void setLugar(String lugar) {
        this.lugar = lugar;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}