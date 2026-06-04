package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Historial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialRepository extends JpaRepository<Historial, Long> {

    List<Historial> findByEvaluacion_IdEvaluacionOrderByFechaCambioDesc(
        Long idEvaluacion);
}