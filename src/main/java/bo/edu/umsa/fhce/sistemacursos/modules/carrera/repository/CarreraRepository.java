package bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.Carrera;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {

    List<Carrera> findByEstado(Carrera.EstadoCarrera estado);
}