package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReporteParticipacionCarreraDto {
    private Long idCarrera;
    private String carrera;
    private long participantesUmsa;
    private long participantesExterno;
    private long total;

    public ReporteParticipacionCarreraDto(
            Long idCarrera,
            String carrera,
            Long participantesUmsa,
            Long participantesExterno) {
        this.idCarrera = idCarrera;
        this.carrera = carrera;
        this.participantesUmsa = participantesUmsa != null ? participantesUmsa : 0L;
        this.participantesExterno = participantesExterno != null ? participantesExterno : 0L;
        this.total = this.participantesUmsa + this.participantesExterno;
    }
}
