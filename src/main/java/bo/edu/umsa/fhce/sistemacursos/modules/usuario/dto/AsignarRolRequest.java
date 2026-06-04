// src/main/java/.../modules/usuario/dto/AsignarRolRequest.java

package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AsignarRolRequest {

    @NotBlank(message = "El nombre del rol es obligatorio")
    // Valores válidos: ADMINISTRADOR, COORDINADOR, DOCENTE, PARTICIPANTE, AUXILIAR, DISEÑADOR
    private String nombreRol;

    // Solo requerido al asignar rol DOCENTE
    private String titulo;

    // Solo requerido al asignar rol PARTICIPANTE
    // Valores: UMSA, EXTERNO
    private String tipoParticipante;
}