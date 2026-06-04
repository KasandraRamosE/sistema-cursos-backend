package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.EvaluacionEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluacionRepository
        extends JpaRepository<EvaluacionEstudiante, Long> {

    Optional<EvaluacionEstudiante> findByInscripcion_IdInscripcion(Long idInscripcion);

    // Todas las evaluaciones de un paralelo específico
    @Query("""
        SELECT e FROM EvaluacionEstudiante e
        WHERE e.inscripcion.curso.idCurso = :idCurso
        AND e.inscripcion.codigoParalelo = :codigo
        """)
    List<EvaluacionEstudiante> findByParalelo(
        @Param("idCurso") Long idCurso,
        @Param("codigo") String codigo
    );

    // Contar aprobados en un paralelo
    @Query("""
        SELECT COUNT(e) FROM EvaluacionEstudiante e
        WHERE e.inscripcion.curso.idCurso = :idCurso
        AND e.inscripcion.codigoParalelo = :codigo
        AND e.estado = 'APROBADO'
        """)
    int contarAprobados(
        @Param("idCurso") Long idCurso,
        @Param("codigo") String codigo
    );

    @Query("""
        SELECT COUNT(e) FROM EvaluacionEstudiante e
        WHERE e.inscripcion.curso.idCurso = :idCurso
        AND e.estado = 'APROBADO'
        """)
    long contarAprobadosPorCurso(@Param("idCurso") Long idCurso);

    // Verificar si todos los inscritos del paralelo tienen nota registrada
    @Query("""
        SELECT COUNT(i) FROM Inscripcion i
        WHERE i.curso.idCurso = :idCurso
        AND i.codigoParalelo = :codigo
        AND i.estado = 'CONFIRMADA'
        AND NOT EXISTS (
            SELECT e FROM EvaluacionEstudiante e
            WHERE e.inscripcion = i
        )
        """)
    int contarSinNota(
        @Param("idCurso") Long idCurso,
        @Param("codigo") String codigo
    );
}