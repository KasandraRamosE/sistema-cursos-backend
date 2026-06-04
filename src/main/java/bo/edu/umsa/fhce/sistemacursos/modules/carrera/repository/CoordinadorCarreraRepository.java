package bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.CoordinadorCarrera;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.CoordinadorCarreraId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CoordinadorCarreraRepository
        extends JpaRepository<CoordinadorCarrera, CoordinadorCarreraId> {

    // Todas las carreras de un coordinador específico
    @Query("SELECT cc FROM CoordinadorCarrera cc WHERE cc.coordinador.idUsuario = :idCoordinador")
    List<CoordinadorCarrera> findByIdCoordinador(@Param("idCoordinador") Long idCoordinador);

    // Todos los coordinadores de una carrera específica
    @Query("SELECT cc FROM CoordinadorCarrera cc WHERE cc.carrera.idCarrera = :idCarrera")
    List<CoordinadorCarrera> findByIdCarrera(@Param("idCarrera") Long idCarrera);

    // Verificar si ya existe la asignación
    boolean existsById(CoordinadorCarreraId id);

    // Verificar si un usuario es coordinador de una carrera
    boolean existsByCoordinador_IdUsuarioAndCarrera_IdCarrera(
        Long idUsuario,
        Long idCarrera
    );
}