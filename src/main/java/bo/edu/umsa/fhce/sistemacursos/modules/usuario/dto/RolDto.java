// src/main/java/.../modules/usuario/dto/RolDto.java

package bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RolDto {
    private Long idRol;
    private String nombre;
    private String descripcion;
}