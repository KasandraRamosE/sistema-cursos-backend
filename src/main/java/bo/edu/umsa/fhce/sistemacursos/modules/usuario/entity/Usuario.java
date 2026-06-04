package bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
@Getter @Setter
@NoArgsConstructor
@Builder                    // permite construir instancias con patrón Builder
@AllArgsConstructor         // necesario cuando usas @Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    // RU para usuarios UMSA, username elegido para externos
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "ci", unique = true, length = 20)
    private String ci;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado = false;

    // NULL para usuarios UMSA (se autentican por API externa)
    // Solo tiene valor para usuarios externos
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 8)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    // Relación N:M con Rol a través de la tabla usuario_rol
    // FetchType.EAGER: carga los roles junto con el usuario
    // (necesario para Spring Security — necesita los roles al autenticar)
    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_rol",
        joinColumns        = @JoinColumn(name = "id_usuario"),
        inverseJoinColumns = @JoinColumn(name = "id_rol")
    )
    private Set<Rol> roles = new HashSet<>();

    // Se ejecuta antes de persistir por primera vez
    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
    }

    // Enum interno para el estado del usuario
    public enum EstadoUsuario {
        ACTIVO, INACTIVO
    }
}