package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity.Aprobacion;

public interface AprobacionRepository extends JpaRepository<Aprobacion, Long> {

    // Última revisión de una plantilla
    Optional<Aprobacion> findTopByPlantilla_IdPlantillaOrderByFechaRevisionDesc(
        Long idPlantilla);

    // Historial completo de revisiones de una plantilla
    List<Aprobacion> findByPlantilla_IdPlantillaOrderByFechaRevisionDesc(
        Long idPlantilla);
}