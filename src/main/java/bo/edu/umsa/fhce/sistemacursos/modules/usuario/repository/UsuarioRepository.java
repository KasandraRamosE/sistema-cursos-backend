package bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// JpaRepository<Entidad, TipoDeLaPK> nos da gratis:
// findById, findAll, save, delete, count, existsById, etc.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Spring genera el SQL automáticamente a partir del nombre del método
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // JPQL con JOIN FETCH para cargar roles en la misma query
    // (evita el problema N+1 de Hibernate)
    @Query("SELECT u FROM Usuario u JOIN FETCH u.roles WHERE u.username = :username")
    Optional<Usuario> findByUsernameWithRoles(@Param("username") String username);

    // Listado por rol con roles cargados en la misma query para evitar N+1
    @Query("""
        SELECT DISTINCT u
        FROM Usuario u
        JOIN FETCH u.roles r
        WHERE UPPER(r.nombre) = UPPER(:nombreRol)
        ORDER BY u.apellidos, u.nombres, u.username
        """)
    List<Usuario> findByRolWithRoles(@Param("nombreRol") String nombreRol);

    long countByPasswordHashIsNull();

    long countByPasswordHashIsNotNull();

    @Modifying
    @Query(value = "UPDATE usuario_rol SET asignado_por = :asignadoPor WHERE id_usuario = :idUsuario AND id_rol = :idRol", nativeQuery = true)
    int actualizarAsignadoPor(
        @Param("idUsuario") Long idUsuario,
        @Param("idRol") Long idRol,
        @Param("asignadoPor") Long asignadoPor
    );
}