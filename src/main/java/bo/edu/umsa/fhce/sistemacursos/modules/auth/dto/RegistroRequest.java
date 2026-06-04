package bo.edu.umsa.fhce.sistemacursos.modules.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistroRequest {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 4, max = 50, message = "El username debe tener entre 4 y 50 caracteres")
    private String username;

    @NotBlank(message = "El carnet de identidad es obligatorio")
    @Size(max = 20, message = "El carnet de identidad es demasiado largo")
    private String ci;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Nombres demasiado largos")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Apellidos demasiado largos")
    private String apellidos;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 120, message = "Email demasiado largo")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    // UMSA o EXTERNO — determina el precio en inscripciones
    @NotNull(message = "El tipo de participante es obligatorio")
    private String tipoParticipante;
}