package bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.CAnulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnulacionRepository extends JpaRepository<CAnulacion, Long> {

    Optional<CAnulacion> findByCertificado_IdCertificado(Long idCertificado);
}