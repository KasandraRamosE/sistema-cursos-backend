package bo.edu.umsa.fhce.sistemacursos.modules.curso.dto;

public class ParaleloDto {
    private String codigo;
    private Long idCurso;
    private Long idDocente;
    private String nombreDocente;   // nombre completo del docente (puede ser null)
    private String tituloDocente;   // titulo academico del docente (puede ser null)
    private String modalidad;
    private Integer cupoMaximo;
    private Integer inscritos;      // calculado en el servicio
    private Integer cuposDisponibles; // cupoMaximo - inscritos (null si sin límite)
    private String horarioDescripcion;
    private String lugar;
    private String link;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Long getIdCurso() {
        return idCurso;
    }

    public void setIdCurso(Long idCurso) {
        this.idCurso = idCurso;
    }

    public Long getIdDocente() {
        return idDocente;
    }

    public void setIdDocente(Long idDocente) {
        this.idDocente = idDocente;
    }

    public String getNombreDocente() {
        return nombreDocente;
    }

    public void setNombreDocente(String nombreDocente) {
        this.nombreDocente = nombreDocente;
    }

    public String getTituloDocente() {
        return tituloDocente;
    }

    public void setTituloDocente(String tituloDocente) {
        this.tituloDocente = tituloDocente;
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

    public Integer getInscritos() {
        return inscritos;
    }

    public void setInscritos(Integer inscritos) {
        this.inscritos = inscritos;
    }

    public Integer getCuposDisponibles() {
        return cuposDisponibles;
    }

    public void setCuposDisponibles(Integer cuposDisponibles) {
        this.cuposDisponibles = cuposDisponibles;
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