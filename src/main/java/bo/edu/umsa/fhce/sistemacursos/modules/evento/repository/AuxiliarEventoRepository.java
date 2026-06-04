// src/main/java/.../modules/evento/repository/AuxiliarEventoRepository.java

package bo.edu.umsa.fhce.sistemacursos.modules.evento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.AuxiliarEvento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.AuxiliarEventoId;

public interface AuxiliarEventoRepository
        extends JpaRepository<AuxiliarEvento, AuxiliarEventoId> {

    // Fetch auxiliar when listing by evento to avoid N+1 when reading usuario fields
    @Query("SELECT ae FROM AuxiliarEvento ae JOIN FETCH ae.auxiliar WHERE ae.evento.idEvento = :idEvento")
    List<AuxiliarEvento> findByIdEvento(@Param("idEvento") Long idEvento);

    // When listing eventos asignados a un auxiliar, fetch the evento and some of its
    // associations to avoid multiple subsequent lazy loads when mapping to DTOs.
    @Query("""
        SELECT ae FROM AuxiliarEvento ae
        JOIN FETCH ae.evento e
        LEFT JOIN FETCH e.carrera
        LEFT JOIN FETCH e.organizador
        LEFT JOIN FETCH e.disenador
        WHERE ae.auxiliar.idUsuario = :idAuxiliar
        ORDER BY e.fechaHora ASC
        """)
    List<AuxiliarEvento> findByIdAuxiliar(@Param("idAuxiliar") Long idAuxiliar);

    // Verifica si un auxiliar está asignado a un evento específico
    // — lo usaremos en el módulo de asistencia para validar permisos
    boolean existsByAuxiliar_IdUsuarioAndEvento_IdEvento(
        Long idAuxiliar, Long idEvento);
}