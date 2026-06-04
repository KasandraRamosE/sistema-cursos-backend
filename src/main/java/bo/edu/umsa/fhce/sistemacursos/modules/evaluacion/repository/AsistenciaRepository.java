package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    Optional<Asistencia> findByInscripcion_IdInscripcion(Long idInscripcion);

    boolean existsByInscripcion_IdInscripcion(Long idInscripcion);

    long countByInscripcion_Evento_IdEvento(Long idEvento);

    // Todos los asistentes de un evento
    @Query("""
        SELECT a FROM Asistencia a
        WHERE a.inscripcion.evento.idEvento = :idEvento
        """)
    List<Asistencia> findByIdEvento(@Param("idEvento") Long idEvento);
}