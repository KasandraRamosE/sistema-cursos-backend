// src/main/java/.../modules/certificado/repository/CertificadoRepository.java

package bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    Optional<Certificado> findByCodigoVerificacion(String codigoVerificacion);

    List<Certificado> findByInscripcion_IdInscripcion(Long idInscripcion);

    Optional<Certificado> findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
        Long idInscripcion,
        Certificado.EstadoEmision estadoEmision
    );

    Optional<Certificado> findFirstByInscripcion_IdInscripcionOrderByVersionDescIdCertificadoDesc(
        Long idInscripcion
    );

    // Certificados de un participante
    List<Certificado> findByInscripcion_Participante_IdUsuario(Long idUsuario);

    // Certificados GENERADOS de un paralelo — para emisión en lote
    @Query("""
        SELECT c FROM Certificado c
        WHERE c.inscripcion.curso.idCurso = :idCurso
        AND c.inscripcion.codigoParalelo = :codigo
        AND c.estadoEmision = 'GENERADO'
        """)
    List<Certificado> findGeneradosPorParalelo(
        @Param("idCurso") Long idCurso,
        @Param("codigo") String codigo
    );

    long countByEstadoEmision(Certificado.EstadoEmision estadoEmision);
}
