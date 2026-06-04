package bo.edu.umsa.fhce.sistemacursos.modules.evento.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import bo.edu.umsa.fhce.sistemacursos.modules.evento.service.EventoEstadoService;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.service.CursoEstadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventoEstadoScheduler {

    private final EventoEstadoService eventoEstadoService;
    private final CursoEstadoService cursoEstadoService;

    @Scheduled(cron = "${app.eventos.finalizacion-cron:0 */5 * * * *}")
    public void finalizarEventosVencidos() {
        int actualizados = eventoEstadoService.sincronizarEventosVencidos();
        if (actualizados > 0) {
            log.info("Scheduler de eventos: {} eventos marcados como FINALIZADO", actualizados);
        }
        int cursosActualizados = cursoEstadoService.sincronizarCursosVencidos();
        if (cursosActualizados > 0) {
            log.info("Scheduler de cursos: {} cursos marcados como FINALIZADO", cursosActualizados);
        }
    }
}