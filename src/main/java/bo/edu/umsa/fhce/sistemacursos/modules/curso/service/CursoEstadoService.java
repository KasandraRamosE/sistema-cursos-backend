package bo.edu.umsa.fhce.sistemacursos.modules.curso.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CursoEstadoService {

    private final CursoRepository cursoRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int sincronizarCursosVencidos() {
        int actualizados = cursoRepository.finalizarCursosVencidos(LocalDate.now());
        if (actualizados > 0) {
            log.info("Cursos finalizados automaticamente por fecha: {}", actualizados);
        }
        return actualizados;
    }
}
