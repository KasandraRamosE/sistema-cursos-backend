package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.SolicitudEmision;

public interface SolicitudEmisionRepository
        extends JpaRepository<SolicitudEmision, Long> {

    // Solicitudes pendientes — bandeja del coordinador
    List<SolicitudEmision> findByEstadoOrderByFechaSolicitudAsc(
        SolicitudEmision.EstadoSolicitud estado);

    List<SolicitudEmision> findAllByOrderByFechaSolicitudDesc();

    // Solicitudes de un docente específico
    List<SolicitudEmision> findByDocente_IdUsuario(Long idDocente);

    boolean existsByEvento_IdEventoAndEstado(
        Long idEvento,
        SolicitudEmision.EstadoSolicitud estado
    );

    boolean existsByCurso_IdCursoAndCodigoParaleloAndEstado(
        Long idCurso,
        String codigoParalelo,
        SolicitudEmision.EstadoSolicitud estado
    );

    boolean existsByCurso_IdCursoAndCodigoParalelo(
        Long idCurso,
        String codigoParalelo
    );

    long countByEstado(SolicitudEmision.EstadoSolicitud estado);
}