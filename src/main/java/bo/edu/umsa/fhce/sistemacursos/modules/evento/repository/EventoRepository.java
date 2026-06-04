package bo.edu.umsa.fhce.sistemacursos.modules.evento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoEventoDto;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByCarrera_IdCarrera(Long idCarrera);

    @Query("""
        SELECT DISTINCT e FROM Evento e
        LEFT JOIN FETCH e.carrera
        LEFT JOIN FETCH e.organizador
        LEFT JOIN FETCH e.disenador
        WHERE e.estado = 'ABIERTO'
        AND (:idCarrera IS NULL OR e.carrera.idCarrera = :idCarrera)
        ORDER BY e.fechaHora ASC
        """)
    List<Evento> findAbiertos(@Param("idCarrera") Long idCarrera);

    List<Evento> findByOrganizador_IdUsuario(Long idUsuario);

    // Eventos asignados a un disenador
    List<Evento> findByDisenador_IdUsuario(Long idUsuario);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Query("""
                UPDATE Evento e
                SET e.estado = 'FINALIZADO'
                WHERE e.estado IN ('ABIERTO', 'LLENO')
                    AND e.fechaHora < :ahora
                """)
        int finalizarEventosVencidos(@Param("ahora") java.time.LocalDateTime ahora);
     
    // Contar inscritos confirmados — temporal hasta tener Inscripcion
    @Query("""
        SELECT COUNT(i) FROM Inscripcion i
        WHERE i.evento.idEvento = :idEvento
        AND i.estado = 'CONFIRMADA'
        """)
    int contarInscritos(@Param("idEvento") Long idEvento);

    long countByEstado(Evento.EstadoEvento estado);

    @Query("""
        SELECT new bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoEventoDto(
            e.idEvento,
            e.nombre,
            e.carrera.nombre,
            e.fechaHora,
            COALESCE(CAST(e.cupoMaximo AS long), 0),
            e.estado,
            COUNT(i)
        )
        FROM Evento e
        LEFT JOIN Inscripcion i
            ON i.evento = e AND i.estado = 'CONFIRMADA'
        WHERE (:idCarrera IS NULL OR e.carrera.idCarrera = :idCarrera)
          AND (:carreras IS NULL OR e.carrera.idCarrera IN :carreras)
          AND (:desde IS NULL OR e.fechaHora >= :desde)
          AND (:hasta IS NULL OR e.fechaHora <= :hasta)
        GROUP BY e.idEvento, e.nombre, e.carrera.nombre, e.fechaHora, e.cupoMaximo, e.estado
        ORDER BY e.fechaHora DESC
        """)
    List<ReporteAcademicoEventoDto> reporteAcademicoEventos(
        @Param("idCarrera") Long idCarrera,
        @Param("carreras") List<Long> carreras,
        @Param("desde") java.time.LocalDateTime desde,
        @Param("hasta") java.time.LocalDateTime hasta
    );
    
}