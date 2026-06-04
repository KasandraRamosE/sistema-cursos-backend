package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByInscripcion_IdInscripcion(Long idInscripcion);

    Optional<Pago> findByReferenciaTransaccion(String referenciaTransaccion);
}