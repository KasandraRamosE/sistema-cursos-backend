// src/main/java/.../modules/plantilla/repository/PlantillaRepository.java

package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity.PlantillaCertificado;

public interface PlantillaRepository
        extends JpaRepository<PlantillaCertificado, Long> {

    // Plantilla VIGENTE de un curso
    Optional<PlantillaCertificado> findByCurso_IdCursoAndEstado(
        Long idCurso,
        PlantillaCertificado.EstadoPlantilla estado
    );

    // Plantilla VIGENTE de un evento
    Optional<PlantillaCertificado> findByEvento_IdEventoAndEstado(
        Long idEvento,
        PlantillaCertificado.EstadoPlantilla estado
    );

    // Todas las plantillas de un curso — historial de versiones
    List<PlantillaCertificado> findByCurso_IdCursoOrderByVersionDesc(Long idCurso);

    // Todas las plantillas de un evento
    List<PlantillaCertificado> findByEvento_IdEventoOrderByVersionDesc(Long idEvento);

    // Plantillas pendientes de revisión — bandeja del coordinador
    List<PlantillaCertificado> findByEstadoOrderByFechaSubidaAsc(
        PlantillaCertificado.EstadoPlantilla estado);

    // Plantillas subidas por un diseñador
    List<PlantillaCertificado> findBySubidaPor_IdUsuario(Long idUsuario);

    // Marcar plantillas anteriores como HISTORICA al aprobar una nueva
    @Modifying
    @Query("""
        UPDATE PlantillaCertificado p
        SET p.estado = 'HISTORICA'
        WHERE p.curso.idCurso = :idCurso
        AND p.estado = 'VIGENTE'
        """)
    void archivarVigentesDeCurso(@Param("idCurso") Long idCurso);

    @Modifying
    @Query("""
        UPDATE PlantillaCertificado p
        SET p.estado = 'HISTORICA'
        WHERE p.curso.idCurso = :idCurso
        AND p.estado = 'PENDIENTE'
        """)
    void archivarPendientesDeCurso(@Param("idCurso") Long idCurso);

    @Modifying
    @Query("""
        UPDATE PlantillaCertificado p
        SET p.estado = 'HISTORICA'
        WHERE p.evento.idEvento = :idEvento
        AND p.estado = 'VIGENTE'
        """)
    void archivarVigentesDeEvento(@Param("idEvento") Long idEvento);

    @Modifying
    @Query("""
        UPDATE PlantillaCertificado p
        SET p.estado = 'HISTORICA'
        WHERE p.evento.idEvento = :idEvento
        AND p.estado = 'PENDIENTE'
        """)
    void archivarPendientesDeEvento(@Param("idEvento") Long idEvento);
}