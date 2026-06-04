package bo.edu.umsa.fhce.sistemacursos.modules.curso.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoCursoDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {

    // Cursos por carrera
    List<Curso> findByCarrera_IdCarrera(Long idCarrera);

    // Cursos abiertos de una carrera — para el catálogo de participantes
    @Query("""
        SELECT DISTINCT c FROM Curso c
        LEFT JOIN FETCH c.carrera
        LEFT JOIN FETCH c.organizador
        LEFT JOIN FETCH c.disenador
        LEFT JOIN FETCH c.paralelos p
        LEFT JOIN FETCH p.docente
        WHERE c.estado = 'ABIERTO'
        AND (:idCarrera IS NULL OR c.carrera.idCarrera = :idCarrera)
        ORDER BY c.fechaInicio ASC
        """)
    List<Curso> findAbiertos(@Param("idCarrera") Long idCarrera);

    // Cursos gestionados por un coordinador específico
    List<Curso> findByOrganizador_IdUsuario(Long idUsuario);

    // Cursos asignados a un disenador
    List<Curso> findByDisenador_IdUsuario(Long idUsuario);

    long countByEstado(Curso.EstadoCurso estado);

        @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
        @org.springframework.data.jpa.repository.Query("""
                UPDATE Curso c
                SET c.estado = 'FINALIZADO'
                WHERE c.estado IN ('ABIERTO', 'LLENO')
                    AND c.fechaInicio < :hoy
                """)
        int finalizarCursosVencidos(@org.springframework.data.repository.query.Param("hoy") java.time.LocalDate hoy);

    @Query("""
        SELECT new bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoCursoDto(
            c.idCurso,
            c.nombre,
            c.carrera.nombre,
            c.fechaInicio,
            (SELECT COALESCE(SUM(COALESCE(p.cupoMaximo, 0)), 0) FROM Paralelo p WHERE p.curso = c),
            c.estado,
            COUNT(i)
        )
        FROM Curso c
        LEFT JOIN Inscripcion i
            ON i.curso = c AND i.estado = 'CONFIRMADA'
        WHERE (:idCarrera IS NULL OR c.carrera.idCarrera = :idCarrera)
          AND (:carreras IS NULL OR c.carrera.idCarrera IN :carreras)
          AND (:desde IS NULL OR c.fechaInicio >= :desde)
          AND (:hasta IS NULL OR c.fechaInicio <= :hasta)
        GROUP BY c.idCurso, c.nombre, c.carrera.nombre, c.fechaInicio, c.estado
        ORDER BY c.fechaInicio DESC
        """)
    List<ReporteAcademicoCursoDto> reporteAcademicoCursos(
        @Param("idCarrera") Long idCarrera,
        @Param("carreras") List<Long> carreras,
        @Param("desde") java.time.LocalDate desde,
        @Param("hasta") java.time.LocalDate hasta
    );
}