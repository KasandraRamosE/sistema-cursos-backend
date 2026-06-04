package bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EmitirLoteRequest {

    // Para emitir en lote para un paralelo completo
    @NotNull
    private Long idCurso;

    @NotBlank
    private String codigoParalelo;
}