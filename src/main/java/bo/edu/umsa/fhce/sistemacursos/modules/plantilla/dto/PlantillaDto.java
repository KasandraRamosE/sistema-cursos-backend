// src/main/java/.../modules/plantilla/dto/PlantillaDto.java

package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlantillaDto {
    private Long idPlantilla;
    private Long idCurso;
    private Long idEvento;
    private String nombreActividad;
    private String tipoActividad;  // CURSO o EVENTO
    private Integer version;
    private String subidaPor;
    private LocalDateTime fechaSubida;
    private String estado;
    private String ultimaObservacion; // feedback del coordinador
}