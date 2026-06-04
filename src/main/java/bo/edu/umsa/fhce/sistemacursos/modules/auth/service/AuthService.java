package bo.edu.umsa.fhce.sistemacursos.modules.auth.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LoginRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LoginResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.LogoutRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.MensajeResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RefreshTokenRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RefreshTokenResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.RegistroRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.VerificarEmailRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.SolicitarResetPasswordRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.VerificarCodigoResetRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.dto.CambiarPasswordRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.entity.CodigoVerificacion;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.entity.RefreshToken;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.integration.UmsaAuthClient;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.integration.UmsaAuthResult;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.integration.UmsaAuthStatus;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.repository.CodigoVerificacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.auth.repository.RefreshTokenRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Participante;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Rol;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.ParticipanteRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.RolRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetailsService;
import bo.edu.umsa.fhce.sistemacursos.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    private final ParticipanteRepository participanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CodigoVerificacionRepository codigoRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;
    private final UmsaAuthClient umsaAuthClient;

    @Value("${app.verificacion.expiracion-horas:24}")
    private int expiracionHoras;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${app.umsa.email-domain:umsa.bo}")
    private String umsaEmailDomain;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername().trim();
        Usuario usuario = usuarioRepository.findByUsernameWithRoles(username).orElse(null);

        if (usuario != null && usuario.getPasswordHash() != null) {
            return loginExterno(request);
        }

        if (!esRu(username) && usuario == null) {
            throw new BusinessException("Usuario no encontrado", 401);
        }

        return loginUmsa(request, usuario);

        
    }
    // ── Registro de usuario externo ──────────────────────────────────────────
    @Transactional
    public MensajeResponse registrar(RegistroRequest request) {

        // 1. Validar unicidad
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El username '" + request.getUsername() + "' ya está en uso", 409);
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("El email '" + request.getEmail() + "' ya está registrado", 409);
        }

        // 2. Buscar rol PARTICIPANTE antes de crear el usuario
        Rol rolParticipante = rolRepository.findByNombre(RolConstantes.PARTICIPANTE)
            .orElseThrow(() -> new RuntimeException("Rol PARTICIPANTE no encontrado en BD"));

        // 3. Crear y guardar el usuario
        Usuario usuario = Usuario.builder()
            .username(request.getUsername())
            .ci(request.getCi().trim())
            .nombres(request.getNombres())
            .apellidos(request.getApellidos())
            .email(request.getEmail())
            .emailVerificado(false)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .estado(Usuario.EstadoUsuario.ACTIVO)
            .build();

        usuario.getRoles().add(rolParticipante);
        usuario = usuarioRepository.save(usuario);  // aquí se genera el ID

        // 4. Crear perfil participante DESPUÉS del save (ya tiene ID)
        Participante participante = new Participante();
        participante.setUsuario(usuario);  // @MapsId toma el ID de aquí
        participante.setTipoParticipante(
            Participante.TipoParticipante.valueOf(request.getTipoParticipante())
        );
        participanteRepository.save(participante);

        // 5. Generar y guardar código de verificación
        codigoRepository.invalidarCodigosAnteriores(
            usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.REGISTRO);

        String codigo = generarCodigo6Digitos();
        CodigoVerificacion codigoVerificacion = CodigoVerificacion.builder()
            .usuario(usuario)
            .codigo(codigo)
            .tipo(CodigoVerificacion.TipoCodigo.REGISTRO)
            .fechaCreacion(LocalDateTime.now())
            .fechaExpiracion(LocalDateTime.now().plusHours(expiracionHoras))
            .usado(false)
            .build();
        codigoRepository.save(codigoVerificacion);

        // 6. Enviar email (async)
        emailService.enviarCodigoVerificacion(
            usuario.getEmail(), usuario.getNombres(), codigo);

        log.info("Usuario registrado: {} — código enviado a {}",
                usuario.getUsername(), usuario.getEmail());

        return new MensajeResponse(
            "Registro exitoso. Revisa tu correo " + request.getEmail() +
            " para verificar tu cuenta."
        );
    }
   
    // ── Verificación de email ────────────────────────────────────────────────
    @Transactional
    public MensajeResponse verificarEmail(VerificarEmailRequest request) {

        // 1. Buscar el usuario
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        // 2. Verificar que no esté ya verificado
        if (usuario.isEmailVerificado()) {
            return new MensajeResponse("Tu cuenta ya está verificada. Puedes iniciar sesión.");
        }

        // 3. Buscar código activo
        CodigoVerificacion codigo = codigoRepository
            .findUltimoCodigoActivo(usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.REGISTRO)
            .orElseThrow(() -> new BusinessException(
                "No hay un código de verificación activo. Solicita uno nuevo.", 400));

        // 4. Verificar que el código coincida y no haya expirado
        if (!codigo.getCodigo().equals(request.getCodigo())) {
            throw new BusinessException("Código de verificación incorrecto", 400);
        }
        if (!codigo.esValido()) {
            throw new BusinessException("El código ha expirado. Solicita uno nuevo.", 400);
        }

        // 5. Activar cuenta y marcar código como usado
        usuario.setEmailVerificado(true);
        usuarioRepository.save(usuario);

        codigo.setUsado(true);
        codigo.setFechaUso(LocalDateTime.now());
        codigoRepository.save(codigo);

        log.info("Email verificado para usuario: {}", usuario.getUsername());

        return new MensajeResponse("¡Cuenta verificada exitosamente! Ya puedes iniciar sesión.");
    }

    // ── Reenviar código ──────────────────────────────────────────────────────
    @Transactional
    public MensajeResponse reenviarCodigo(String username) {

        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        if (usuario.isEmailVerificado()) {
            throw new BusinessException("Esta cuenta ya está verificada.", 400);
        }

        // Invalidar anteriores y generar nuevo
        codigoRepository.invalidarCodigosAnteriores(
            usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.REGISTRO);

        String nuevoCodigo = generarCodigo6Digitos();

        CodigoVerificacion codigo = CodigoVerificacion.builder()
            .usuario(usuario)
            .codigo(nuevoCodigo)
            .tipo(CodigoVerificacion.TipoCodigo.REGISTRO)
            .usado(false)
            .fechaCreacion(LocalDateTime.now())
            .fechaExpiracion(LocalDateTime.now().plusHours(expiracionHoras))
            .build();
        codigoRepository.save(codigo);

        emailService.enviarCodigoVerificacion(
            usuario.getEmail(), usuario.getNombres(), nuevoCodigo);

        return new MensajeResponse("Se envió un nuevo código a " + usuario.getEmail());
    }

    private LoginResponse loginExterno(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Usuario usuario = usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
        if (usuario.getEstado() == Usuario.EstadoUsuario.INACTIVO) {
            throw new BusinessException("La cuenta está inactiva", 403);
        }

        return buildLoginResponse(userDetails, usuario);
    }

    private LoginResponse loginUmsa(LoginRequest request, Usuario usuario) {
        UmsaAuthResult result = umsaAuthClient.authenticate(
            request.getUsername().trim(),
            request.getPassword()
        );

        if (result.getStatus() == UmsaAuthStatus.UNAVAILABLE) {
            throw new BusinessException("Servicio de autenticación UMSA no disponible", 503);
        }

        if (result.getStatus() != UmsaAuthStatus.SUCCESS) {
            if (usuario != null && usuario.getPasswordHash() == null
                && (result.getStatus() == UmsaAuthStatus.NOT_FOUND || result.getStatus() == UmsaAuthStatus.INACTIVE)) {
                usuario.setEstado(Usuario.EstadoUsuario.INACTIVO);
                usuarioRepository.save(usuario);
            }
            throw new BusinessException("Credenciales UMSA inválidas", 401);
        }

        Usuario actualizado = sincronizarUsuarioUmsa(usuario, result);
        CustomUserDetails userDetails = new CustomUserDetails(actualizado);
        return buildLoginResponse(userDetails, actualizado);
    }

    private Usuario sincronizarUsuarioUmsa(Usuario usuario, UmsaAuthResult result) {
        String ru = result.getRu();
        String email = construirEmailUmsa(ru);

        if (usuario == null) {
            if (usuarioRepository.existsByEmail(email)) {
                throw new BusinessException("El email " + email + " ya está registrado", 409);
            }
            Rol rolParticipante = rolRepository.findByNombre(RolConstantes.PARTICIPANTE)
                .orElseThrow(() -> new RuntimeException("Rol PARTICIPANTE no encontrado en BD"));

            Usuario nuevoUsuario = Usuario.builder()
                .username(ru)
                .nombres(result.getNombres())
                .apellidos(result.getApellidos())
                .email(email)
                .emailVerificado(true)
                .passwordHash(null)
                .estado(Usuario.EstadoUsuario.ACTIVO)
                .build();

            nuevoUsuario.getRoles().add(rolParticipante);
            nuevoUsuario = usuarioRepository.save(nuevoUsuario);

            Participante participante = new Participante();
            participante.setUsuario(nuevoUsuario);
            participante.setTipoParticipante(Participante.TipoParticipante.UMSA);
            participanteRepository.save(participante);

            return nuevoUsuario;
        }

        if (!email.equals(usuario.getEmail()) && usuarioRepository.existsByEmail(email)) {
            throw new BusinessException("El email " + email + " ya está registrado", 409);
        }

        usuario.setNombres(result.getNombres());
        usuario.setApellidos(result.getApellidos());
        usuario.setEmail(email);
        usuario.setEmailVerificado(true);
        usuario.setEstado(Usuario.EstadoUsuario.ACTIVO);

        if (usuario.getRoles().stream().noneMatch(r -> .equals(r.getNombre()))) {
            Rol rolParticipante = rolRepository.findByNombre(RolConstantes.PARTICIPANTE)
                .orElseThrow(() -> new RuntimeException("Rol PARTICIPANTE no encontrado en BD"));
            usuario.getRoles().add(rolParticipante);
        }

        usuario = usuarioRepository.save(usuario);

        if (participanteRepository.findById(usuario.getIdUsuario()).isEmpty()) {
            Participante participante = new Participante();
            participante.setUsuario(usuario);
            participante.setTipoParticipante(Participante.TipoParticipante.UMSA);
            participanteRepository.save(participante);
        }

        return usuario;
    }

    private LoginResponse buildLoginResponse(CustomUserDetails userDetails, Usuario usuario) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );

        String jwt = tokenProvider.generateToken(authentication);

        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        String tipoParticipante = participanteRepository.findById(userDetails.getIdUsuario())
            .map(participante -> participante.getTipoParticipante().name())
            .orElse(null);

        RefreshToken refreshToken = crearRefreshToken(usuario);

        return new LoginResponse(
            jwt,
            "Bearer",
            refreshToken.getToken(),
            userDetails.getIdUsuario(),
            userDetails.getUsername(),
            userDetails.getNombres(),
            userDetails.getApellidos(),
            tipoParticipante,
            roles
        );
    }

    // ── Refresh token ───────────────────────────────────────────────────────
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
            .findByTokenAndRevocadoFalse(request.getRefreshToken())
            .orElseThrow(() -> new BusinessException("Refresh token inválido", 401));

        if (refreshToken.estaExpirado()) {
            refreshToken.setRevocado(true);
            refreshTokenRepository.save(refreshToken);
            throw new BusinessException("Refresh token expirado", 401);
        }

        Usuario usuario = refreshToken.getUsuario();
        if (usuario.getEstado() == Usuario.EstadoUsuario.INACTIVO) {
            throw new BusinessException("La cuenta está inactiva", 403);
        }

        CustomUserDetails userDetails = (CustomUserDetails)
            userDetailsService.loadUserByUsername(usuario.getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );

        String jwt = tokenProvider.generateToken(authentication);

        refreshToken.setRevocado(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken nuevoRefresh = crearRefreshToken(usuario);

        return new RefreshTokenResponse(jwt, "Bearer", nuevoRefresh.getToken());
    }

    // ── Logout ─────────────────────────────────────────────────────────────
    @Transactional
    public MensajeResponse logout(LogoutRequest request) {
        refreshTokenRepository.revocarPorToken(request.getRefreshToken());
        return new MensajeResponse("Sesión cerrada correctamente");
    }

    // ── Reset Password: Solicitar código ────────────────────────────────────
    @Transactional
    public MensajeResponse solicitarResetPassword(SolicitarResetPasswordRequest request) {
        String username = request.getUsername().trim();
        
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        // Validar que sea usuario EXTERNO (con hash de contraseña)
        if (usuario.getPasswordHash() == null) {
            throw new BusinessException(
                "Los usuarios UMSA deben recuperar su contraseña a través del portal institucional", 403);
        }

        // Validar que la cuenta esté activa
        if (usuario.getEstado() == Usuario.EstadoUsuario.INACTIVO) {
            throw new BusinessException("La cuenta está inactiva", 403);
        }

        // Invalidar códigos anteriores
        codigoRepository.invalidarCodigosAnteriores(
            usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.RESET_PASSWORD);

        // Generar nuevo código
        String codigo = generarCodigo6Digitos();
        CodigoVerificacion codigoVerificacion = CodigoVerificacion.builder()
            .usuario(usuario)
            .codigo(codigo)
            .tipo(CodigoVerificacion.TipoCodigo.RESET_PASSWORD)
            .fechaCreacion(LocalDateTime.now())
            .fechaExpiracion(LocalDateTime.now().plusHours(expiracionHoras))
            .usado(false)
            .build();
        codigoRepository.save(codigoVerificacion);

        // Enviar email
        emailService.enviarCodigoVerificacion(
            usuario.getEmail(), usuario.getNombres(), codigo);

        log.info("Solicitud de reset de contraseña para usuario: {}", username);

        return new MensajeResponse(
            "Se envió un código a tu correo " + usuario.getEmail() +
            " para cambiar tu contraseña.");
    }

    // ── Reset Password: Verificar código ────────────────────────────────────
    @Transactional
    public MensajeResponse verificarCodigoReset(VerificarCodigoResetRequest request) {
        String username = request.getUsername().trim();
        
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        // Buscar código activo
        CodigoVerificacion codigo = codigoRepository
            .findUltimoCodigoActivo(usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.RESET_PASSWORD)
            .orElseThrow(() -> new BusinessException(
                "No hay un código de reset activo. Solicita uno nuevo.", 400));

        // Verificar código
        if (!codigo.getCodigo().equals(request.getCodigo())) {
            throw new BusinessException("Código incorrecto", 400);
        }

        if (!codigo.esValido()) {
            throw new BusinessException("El código ha expirado. Solicita uno nuevo.", 400);
        }

        log.info("Código de reset verificado para usuario: {}", username);

        return new MensajeResponse("Código verificado. Ya puedes cambiar tu contraseña.");
    }

    // ── Reset Password: Cambiar contraseña ──────────────────────────────────
    @Transactional
    public MensajeResponse cambiarPassword(CambiarPasswordRequest request) {
        String username = request.getUsername().trim();
        
        // Buscar usuario
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Usuario no encontrado", 404));

        // Validar que sea usuario EXTERNO
        if (usuario.getPasswordHash() == null) {
            throw new BusinessException("No puedes cambiar la contraseña de una cuenta UMSA", 403);
        }

        // Buscar código
        CodigoVerificacion codigo = codigoRepository
            .findUltimoCodigoActivo(usuario.getIdUsuario(), CodigoVerificacion.TipoCodigo.RESET_PASSWORD)
            .orElseThrow(() -> new BusinessException(
                "No hay un código de reset activo. Solicita uno nuevo.", 400));

        // Verificar código
        if (!codigo.getCodigo().equals(request.getCodigo())) {
            throw new BusinessException("Código incorrecto", 400);
        }

        if (!codigo.esValido()) {
            throw new BusinessException("El código ha expirado. Solicita uno nuevo.", 400);
        }

        // Cambiar contraseña
        usuario.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);

        // Marcar código como usado
        codigo.setUsado(true);
        codigo.setFechaUso(LocalDateTime.now());
        codigoRepository.save(codigo);

        log.info("Contraseña cambiada para usuario: {}", username);

        return new MensajeResponse("Contraseña cambiada exitosamente. Ya puedes iniciar sesión.");
    }

    // ── Utilidad: genera código numérico de 6 dígitos ───────────────────────
    private String generarCodigo6Digitos() {
        // SecureRandom es criptográficamente seguro (mejor que Math.random())
        SecureRandom random = new SecureRandom();
        int numero = 100000 + random.nextInt(900000); // rango: 100000–999999
        return String.valueOf(numero);
    }

    private RefreshToken crearRefreshToken(Usuario usuario) {
        String token = generarTokenSeguro();
        RefreshToken refreshToken = RefreshToken.builder()
            .usuario(usuario)
            .token(token)
            .revocado(false)
            .fechaCreacion(LocalDateTime.now())
            .fechaExpiracion(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
            .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private String generarTokenSeguro() {
        byte[] randomBytes = new byte[64];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private boolean esRu(String username) {
        return username != null && username.matches("\\d+");
    }

    private String construirEmailUmsa(String ru) {
        return ru + "@" + umsaEmailDomain;
    }
    public class RolConstantes {
        public static final String PARTICIPANTE = "PARTICIPANTE";
    }

}