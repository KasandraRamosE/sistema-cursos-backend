package bo.edu.umsa.fhce.sistemacursos.modules.curso.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId;

public interface ParaleloRepository extends JpaRepository<Paralelo, ParaleloId> {

    // Todos los paralelos de un curso
    List<Paralelo> findById_IdCurso(Long idCurso);

    // Todos los paralelos asignados a un docente
    List<Paralelo> findByDocente_IdUsuario(Long idUsuario);

    @Query("""
        SELECT COALESCE(SUM(COALESCE(p.cupoMaximo, 0)), 0)
        FROM Paralelo p
        WHERE p.curso.idCurso = :idCurso
        """)
    Integer sumarCupoMaximo(@Param("idCurso") Long idCurso);
    /* 
    // Contar inscritos confirmados en un paralelo específico
    @Query("""
        SELECT COUNT(i) FROM Inscripcion i
        WHERE i.curso.idCurso = :idCurso
        AND i.codigoParalelo = :codigo
        AND i.estado = 'CONFIRMADA'
        """)
    int contarInscritosConfirmados(
        @Param("idCurso") Long idCurso,
        @Param("codigo") String codigo
    );
    */
   // ParaleloRepository.java — versión temporal
    default int contarInscritosConfirmados(Long idCurso, String codigo) {
        return 0; // se implementa cuando exista la entidad Inscripcion
    }
}