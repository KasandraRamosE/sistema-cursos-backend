package bo.edu.umsa.fhce.sistemacursos.modules.evento.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventoRequest {

    @NotNull(message = "La carrera es obligatoria")
    private Long idCarrera;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 200)
    private String nombre;

    private String descripcion;

    @Size(max = 255)
    private String lugar;

    @Size(max = 255)
    private String imagen;

    @NotNull(message = "La carga horaria es obligatoria")
    @Positive(message = "La carga horaria debe ser mayor a 0")
    private Integer cargaHoraria;

    @NotNull(message = "La modalidad es obligatoria")
    private String modalidad;

    @NotNull(message = "La fecha y hora son obligatorias")
    @Future(message = "La fecha del evento debe ser futura")
    private LocalDateTime fechaHora;

    @Positive(message = "El cupo debe ser mayor a 0")
    private Integer cupoMaximo;

    @NotNull(message = "El costo externo es obligatorio")
    @DecimalMin(value = "0.00")
    private BigDecimal costoExterno;

    @NotNull(message = "El costo UMSA es obligatorio")
    @DecimalMin(value = "0.00")
    private BigDecimal costoUmsa;

    @Size(max = 255)
    private String link;
}