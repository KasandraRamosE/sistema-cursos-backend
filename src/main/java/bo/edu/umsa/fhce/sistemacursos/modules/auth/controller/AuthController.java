package bo.edu.umsa.fhce.sistemacursos.modules.auth.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LoginRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LoginResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LogoutRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.MensajeResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RefreshTokenRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RefreshTokenResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.VerificarEmailRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RegistroRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.SolicitarResetPasswordRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.VerificarCodigoResetRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.CambiarPasswordRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.ReenviarCodigoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.service.AuthService;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

// AuthController.java — versión completa

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, registro y verificación de usuarios")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devuelve JWT para usuarios UMSA y externos")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar token", description = "Devuelve un nuevo JWT usando refresh token")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoca el refresh token actual")
    public ResponseEntity<MensajeResponse> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(authService.logout(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registro de usuario externo")
    public ResponseEntity<MensajeResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        // 201 Created para creación de recursos
        return ResponseEntity.status(201).body(authService.registrar(request));
    }

    @PostMapping("/verificar-email")
    @Operation(summary = "Verificar email con código de 6 dígitos")
    public ResponseEntity<MensajeResponse> verificarEmail(
            @Valid @RequestBody VerificarEmailRequest request) {
        return ResponseEntity.ok(authService.verificarEmail(request));
    }

    @PostMapping("/reenviar-codigo")
    @Operation(summary = "Reenviar código de verificación")
    public ResponseEntity<MensajeResponse> reenviarCodigo(
            @Valid @RequestBody ReenviarCodigoRequest request) {
        return ResponseEntity.ok(authService.reenviarCodigo(request.getUsername()));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado actual")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();
        response.put("idUsuario",  user.getIdUsuario());
        response.put("username",   user.getUsername());
        response.put("nombres",    user.getNombres());
        response.put("apellidos",  user.getApellidos());
        response.put("roles", user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority).toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/solicitar-reset-password")
    @Operation(summary = "Solicitar código para resetear contraseña")
    public ResponseEntity<MensajeResponse> solicitarResetPassword(
            @Valid @RequestBody SolicitarResetPasswordRequest request) {
        return ResponseEntity.ok(authService.solicitarResetPassword(request));
    }

    @PostMapping("/verificar-codigo-reset")
    @Operation(summary = "Verificar código de reset de contraseña")
    public ResponseEntity<MensajeResponse> verificarCodigoReset(
            @Valid @RequestBody VerificarCodigoResetRequest request) {
        return ResponseEntity.ok(authService.verificarCodigoReset(request));
    }

    @PostMapping("/cambiar-password")
    @Operation(summary = "Cambiar contraseña con código verificado")
    public ResponseEntity<MensajeResponse> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request) {
        return ResponseEntity.ok(authService.cambiarPassword(request));
    }
}