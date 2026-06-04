package bo.edu.umsa.fhce.sistemacursos.security;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Spring Security llama a este servicio para cargar un usuario por username.
// Lo usa durante la autenticación y para verificar el token JWT en cada request.
@Service
@RequiredArgsConstructor // Lombok genera constructor con todos los campos final
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository
            .findByUsernameWithRoles(username) // JOIN FETCH para cargar roles
            .map(CustomUserDetails::new)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuario no encontrado: " + username));
    }
}