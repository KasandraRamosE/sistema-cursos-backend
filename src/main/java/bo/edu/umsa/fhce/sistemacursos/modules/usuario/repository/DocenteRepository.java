package bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Docente;

public interface DocenteRepository extends JpaRepository<Docente, Long> {

	@Query("""
		SELECT d FROM Docente d
		WHERE d.idUsuario IN :ids
		""")
	List<Docente> findByIdUsuarioIn(@Param("ids") Collection<Long> ids);
}