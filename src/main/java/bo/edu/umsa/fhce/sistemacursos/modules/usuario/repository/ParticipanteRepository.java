package bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Participante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
}