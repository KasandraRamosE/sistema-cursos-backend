package bo.edu.umsa.fhce.sistemacursos.modules.auth.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.auth.entity.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, Long> {

    // Busca el código más reciente no usado para un usuario
    @Query("""
        SELECT c FROM CodigoVerificacion c
        WHERE c.usuario.idUsuario = :idUsuario
          AND c.tipo = :tipo
          AND c.usado = false
        ORDER BY c.fechaCreacion DESC
        LIMIT 1
        """)
    Optional<CodigoVerificacion> findUltimoCodigoActivo(
        @Param("idUsuario") Long idUsuario,
        @Param("tipo") CodigoVerificacion.TipoCodigo tipo
    );

    // Invalida todos los códigos anteriores del usuario antes de generar uno nuevo
    @Modifying
    @Query("""
        UPDATE CodigoVerificacion c
        SET c.usado = true
        WHERE c.usuario.idUsuario = :idUsuario
          AND c.tipo = :tipo
          AND c.usado = false
        """)
    void invalidarCodigosAnteriores(
        @Param("idUsuario") Long idUsuario,
        @Param("tipo") CodigoVerificacion.TipoCodigo tipo
    );
}