package bo.edu.umsa.fhce.sistemacursos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

// Responsabilidad única: generar, validar y leer tokens JWT.
// Ninguna otra clase conoce el formato interno del token.
@Component
public class JwtTokenProvider {

    // Inyectados desde application.yml → app.jwt.*
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // Convierte el String del secret en una clave criptográfica HMAC-SHA256
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Genera un JWT firmado con:
    // - subject: username del usuario
    // - claim "roles": lista de nombres de roles (para autorización)
    // - claim "userId": id del usuario (para lookup sin ir a BD en cada request)
    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Lista de roles como strings: ["ADMINISTRADOR", "COORDINADOR"]
        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        return Jwts.builder()
            .subject(userDetails.getUsername())
            .claim("userId", userDetails.getIdUsuario())
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    // Extrae el username del token (usado en el filtro para cargar el usuario)
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    // Verifica firma, expiración y formato — devuelve true si el token es válido
    public boolean validateToken(String token) {
        try {
            parseClaims(token); // si no lanza excepción, es válido
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Token inválido, expirado, o mal formado — se loguea pero no se propaga
            return false;
        }
    }

    // Parsea el token y extrae los Claims (payload)
    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}