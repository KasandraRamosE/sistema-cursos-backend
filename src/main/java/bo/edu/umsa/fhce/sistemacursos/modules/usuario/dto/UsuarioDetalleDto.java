// Para ver un usuario específico — incluye perfil docente/participante

package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter 
@Setter
public class UsuarioDetalleDto {
    private Long idUsuario;
    private String username;
    private String ci;
    private String nombres;
    private String apellidos;
    private String email;
    private String tipoUsuario;
    private boolean emailVerificado;
    private String estado;
    private LocalDateTime fechaRegistro;
    private List<RolDto> roles;

    // Solo presente si tiene rol DOCENTE
    private String titulo;

    // Solo presente si tiene rol PARTICIPANTE
    private String tipoParticipante;
}