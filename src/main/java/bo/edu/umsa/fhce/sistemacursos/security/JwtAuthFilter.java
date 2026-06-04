package bo.edu.umsa.fhce.sistemacursos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// Filtro que se ejecuta UNA VEZ por cada request HTTP.
// Responsabilidad: leer el JWT del header, validarlo, y si es válido
// establecer la autenticación en el SecurityContext para ese request.
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer el token del header Authorization: Bearer <token>
        String token = extractTokenFromRequest(request);

        // 2. Si hay token y es válido → establecer autenticación
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {

            String username = tokenProvider.getUsernameFromToken(token);

            // Carga el usuario desde BD (necesario para tener los roles actualizados)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Crea el objeto de autenticación de Spring Security
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,                          // credentials null (ya autenticado)
                    userDetails.getAuthorities()   // roles del usuario
                );
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));

            // Registra la autenticación para este request
            // A partir de aquí, @PreAuthorize y SecurityContextHolder funcionan
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. Continúa la cadena de filtros (siempre, con o sin token)
        filterChain.doFilter(request, response);
    }

    // Extrae el token del header: "Bearer eyJhbGci..." → "eyJhbGci..."
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // remueve "Bearer "
        }
        return null;
    }
}