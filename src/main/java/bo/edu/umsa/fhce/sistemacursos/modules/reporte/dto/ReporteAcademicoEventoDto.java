package bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto;

import java.time.LocalDateTime;

import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteAcademicoEventoDto {
    private Long idEvento;
    private String nombre;
    private String carrera;
    private LocalDateTime fechaHora;
    private Long cupoMaximo;
    private Evento.EstadoEvento estado;
    private Long inscritosConfirmados;
}
