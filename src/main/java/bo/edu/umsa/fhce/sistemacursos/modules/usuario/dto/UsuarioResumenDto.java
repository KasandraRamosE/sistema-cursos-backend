package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UsuarioResumenDto {
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
    private List<String> roles;  // solo los nombres: ["COORDINADOR", "DOCENTE"]
}