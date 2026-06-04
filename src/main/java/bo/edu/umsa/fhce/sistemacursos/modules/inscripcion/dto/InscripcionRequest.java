package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InscripcionRequest {

    // Solo uno de estos dos debe tener valor
    private Long idCurso;
    private Long idEvento;

    // Requerido solo si es inscripción a curso
    private String codigoParalelo;
}