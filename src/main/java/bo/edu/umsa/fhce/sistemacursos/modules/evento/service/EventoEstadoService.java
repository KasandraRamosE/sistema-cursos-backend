package bo.edu.umsa.fhce.sistemacursos.modules.evento.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventoEstadoService {

    private final EventoRepository eventoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int sincronizarEventosVencidos() {
        int actualizados = eventoRepository.finalizarEventosVencidos(LocalDateTime.now());
        if (actualizados > 0) {
            log.info("Eventos finalizados automaticamente por fecha: {}", actualizados);
        }
        return actualizados;
    }
}