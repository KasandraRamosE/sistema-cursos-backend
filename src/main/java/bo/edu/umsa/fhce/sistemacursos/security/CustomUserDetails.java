package bo.edu.umsa.fhce.sistemacursos.security;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.text.Normalizer;
import java.util.Collection;
import java.util.stream.Collectors;

// Adaptador entre nuestra entidad Usuario y lo que Spring Security necesita.
// Spring Security trabaja con UserDetails — no sabe nada de nuestro Usuario.
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long idUsuario;   // campo extra que necesitamos en el token
    private final String username;
    private final String password;  // passwordHash para externos, null para UMSA
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    private final String nombres;
    private final String apellidos;
    private final String email;

    // Construimos desde nuestra entidad Usuario
    public CustomUserDetails(Usuario usuario) {
        this.idUsuario = usuario.getIdUsuario();
        this.username  = usuario.getUsername();
        this.password  = usuario.getPasswordHash(); // puede ser null para UMSA
        this.enabled   = usuario.getEstado() == Usuario.EstadoUsuario.ACTIVO;
        this.nombres    = usuario.getNombres();
        this.apellidos  = usuario.getApellidos();
        this.email      = usuario.getEmail();
        // Convertimos los Rol del usuario en GrantedAuthority de Spring
        // Spring Security espera el prefijo "ROLE_" para roles
        this.authorities = usuario.getRoles().stream()
            .map(rol -> new SimpleGrantedAuthority("ROLE_" + normalizeRoleName(rol.getNombre())))
            .collect(Collectors.toSet());
    }

    private String normalizeRoleName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim();
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return normalized.toUpperCase();
    }

    // Spring Security usa estos métodos para validar la cuenta
    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}