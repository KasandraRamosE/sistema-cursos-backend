// src/main/java/.../modules/usuario/service/UsuarioService.java

package bo.edu.umsa.fhce.sistemacursos.modules.usuario.service;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.*;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.*;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.Carrera;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.CarreraRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.CoordinadorCarreraRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.CoordinadorCarrera;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.AuxiliarEvento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.AuxiliarEventoId;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.AuxiliarEventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.ParaleloRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository    usuarioRepository;
    private final RolRepository        rolRepository;
    private final DocenteRepository    docenteRepository;
    private final ParticipanteRepository participanteRepository;
    private final CarreraRepository    carreraRepository;
    private final CoordinadorCarreraRepository coordinadorCarreraRepository;
    private final EventoRepository     eventoRepository;
    private final AuxiliarEventoRepository auxiliarEventoRepository;
    private final ParaleloRepository   paraleloRepository;
    private final ModelMapper          modelMapper;
    private final PasswordEncoder passwordEncoder;

    // ── Listar todos los usuarios ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UsuarioResumenDto> listarUsuarios() {
        return usuarioRepository.findAll().stream()
            .map(this::toResumenDto)
            .toList();
    }

    // ── Listar usuarios por rol ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<UsuarioResumenDto> listarPorRol(String nombreRol) {
        String rolDB = findRolByNombreCompat(nombreRol)
            .map(Rol::getNombre)
            .orElse(normalizeRolName(nombreRol));

        return usuarioRepository.findByRolWithRoles(rolDB).stream()
            .map(this::toResumenDto)
            .toList();
    }

    // ── Ver detalle de un usuario ────────────────────────────────────────────
    @Transactional(readOnly = true)
    public UsuarioDetalleDto obtenerDetalle(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        return toDetalleDto(usuario);
    }

    // ── Perfil del usuario autenticado ─────────────────────────────────────
    @Transactional(readOnly = true)
    public UsuarioDetalleDto obtenerActual() {
        Usuario usuario = buscarUsuario(requireUsuarioActualId());
        return toDetalleDto(usuario);
    }

    // ── Actualizar perfil (solo externo) ───────────────────────────────────
    @Transactional
    public UsuarioDetalleDto actualizarPerfilExterno(ActualizarPerfilRequest request) {
        Usuario usuario = buscarUsuario(requireUsuarioActualId());
        validarExterno(usuario);

        usuario.setCi(request.getCi().trim());
        usuario.setNombres(request.getNombres().trim());
        usuario.setApellidos(request.getApellidos().trim());
        usuarioRepository.save(usuario);

        return toDetalleDto(usuario);
    }

    // ── Cambiar contrasena (solo externo) ─────────────────────────────────-
    @Transactional
    public void cambiarPasswordExterno(CambiarPasswordRequest request) {
        Usuario usuario = buscarUsuario(requireUsuarioActualId());
        validarExterno(usuario);

        if (usuario.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPasswordActual(), usuario.getPasswordHash())) {
            throw new BusinessException("Contrasena actual incorrecta", 400);
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.getPasswordNueva()));
        usuarioRepository.save(usuario);
    }

    // ── Cambiar estado ACTIVO / INACTIVO ────────────────────────────────────
    @Transactional
    public UsuarioResumenDto cambiarEstado(Long idUsuario, CambiarEstadoRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        Usuario.EstadoUsuario nuevoEstado;
        try {
            nuevoEstado = Usuario.EstadoUsuario.valueOf(request.getEstado());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                "Estado inválido: " + request.getEstado() + ". Use ACTIVO o INACTIVO", 400);
        }

        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);

        log.info("Estado del usuario {} cambiado a {}", usuario.getUsername(), nuevoEstado);
        return toResumenDto(usuario);
    }

    // ── Asignar rol a usuario ────────────────────────────────────────────────
    @Transactional
    public UsuarioDetalleDto asignarRol(Long idUsuario, AsignarRolRequest request) {
        Usuario usuario = buscarUsuario(idUsuario);

        String rolSolicitado = normalizeRolName(request.getNombreRol());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean esCoordinador = authentication != null
            && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_COORDINADOR"));

        if (esCoordinador && !rolSolicitado.equals("DOCENTE")
                && !rolSolicitado.equals("AUXILIAR")
                && !rolSolicitado.equals("DISENADOR")) {
            throw new BusinessException(
            "Solo puedes asignar roles DOCENTE, AUXILIAR o DISENADOR", 403);
        }

        // Verificar que el rol existe
        Rol rol = findRolByNombreCompat(request.getNombreRol())
            .orElseThrow(() -> new BusinessException(
                "Rol no encontrado: " + request.getNombreRol(), 404));

        // Verificar que no tenga el rol ya
        boolean yaLoTiene = usuario.getRoles().stream()
            .anyMatch(r -> normalizeRolName(r.getNombre()).equals(rolSolicitado));
        if (yaLoTiene) {
            throw new BusinessException(
                "El usuario ya tiene el rol " + request.getNombreRol(), 409);
        }

        // Lógica especial según el rol asignado
        switch (rolSolicitado) {

            case "DOCENTE" -> {
                // Requiere título académico
                if (request.getTitulo() == null || request.getTitulo().isBlank()) {
                    throw new BusinessException(
                        "El título académico es obligatorio para el rol DOCENTE", 400);
                }
                Docente docente = new Docente();
                docente.setUsuario(usuario);
                docente.setTitulo(request.getTitulo());
                docenteRepository.save(docente);
            }

            case "PARTICIPANTE" -> {
                // Requiere tipo (UMSA o EXTERNO)
                if (request.getTipoParticipante() == null || request.getTipoParticipante().isBlank()) {
                    throw new BusinessException(
                        "El tipo de participante (UMSA/EXTERNO) es obligatorio", 400);
                }
                // Verificar que no tenga ya perfil de participante
                if (!participanteRepository.findById(idUsuario).isPresent()) {
                    Participante participante = new Participante();
                    participante.setUsuario(usuario);
                    participante.setTipoParticipante(
                        Participante.TipoParticipante.valueOf(request.getTipoParticipante()));
                    participanteRepository.save(participante);
                }
            }
        }

        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);

        Long asignadoPor = getUsuarioActualId();
        if (asignadoPor != null) {
            usuarioRepository.actualizarAsignadoPor(
                usuario.getIdUsuario(), rol.getIdRol(), asignadoPor);
        }

        log.info("Rol {} asignado al usuario {}", request.getNombreRol(), usuario.getUsername());
        return toDetalleDto(usuario);
    }

    // ── Revocar rol de usuario ───────────────────────────────────────────────
    @Transactional
    public UsuarioDetalleDto revocarRol(Long idUsuario, String nombreRol) {
        Usuario usuario = buscarUsuario(idUsuario);
        String rolNormalizado = normalizeRolName(nombreRol);

        // No permitir quitarle el último rol
        if (usuario.getRoles().size() <= 1) {
            throw new BusinessException(
                "No se puede revocar el último rol del usuario", 400);
        }

        findRolByNombreCompat(nombreRol)
            .orElseThrow(() -> new BusinessException("Rol no encontrado: " + nombreRol, 404));

        boolean tienEelRol = usuario.getRoles().removeIf(
            r -> normalizeRolName(r.getNombre()).equals(rolNormalizado));

        if (!tienEelRol) {
            throw new BusinessException(
                "El usuario no tiene el rol " + nombreRol, 400);
        }

        // Si se revoca DOCENTE, eliminar perfil de docente
        if (nombreRol.equals("DOCENTE")) {
            docenteRepository.deleteById(idUsuario);
        }

        usuarioRepository.save(usuario);

        log.info("Rol {} revocado del usuario {}", nombreRol, usuario.getUsername());
        return toDetalleDto(usuario);
    }

    // ── Listar todos los roles disponibles ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<RolDto> listarRoles() {
        return rolRepository.findAll().stream()
            .map(r -> modelMapper.map(r, RolDto.class))
            .toList();
    }

    // ── Carreras del coordinador ───────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Long> listarCarrerasCoordinador(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "COORDINADOR");

        return coordinadorCarreraRepository.findByIdCoordinador(idUsuario)
            .stream()
            .map(cc -> cc.getCarrera().getIdCarrera())
            .toList();
    }

    @Transactional
    public void actualizarCarrerasCoordinador(Long idUsuario, List<Long> carreraIds) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "COORDINADOR");

        Set<Long> nuevas = new HashSet<>(carreraIds);
        List<CoordinadorCarrera> actuales = coordinadorCarreraRepository.findByIdCoordinador(idUsuario);
        Set<Long> actualesIds = actuales.stream()
            .map(cc -> cc.getCarrera().getIdCarrera())
            .collect(Collectors.toSet());

        for (CoordinadorCarrera actual : actuales) {
            if (!nuevas.contains(actual.getCarrera().getIdCarrera())) {
                coordinadorCarreraRepository.delete(actual);
            }
        }

        for (Long idCarrera : nuevas) {
            if (!actualesIds.contains(idCarrera)) {
                Carrera carrera = carreraRepository.findById(idCarrera)
                    .orElseThrow(() -> new ResourceNotFoundException("Carrera", idCarrera));
                CoordinadorCarrera asignacion = new CoordinadorCarrera(usuario, carrera);
                coordinadorCarreraRepository.save(asignacion);
            }
        }
    }

    // ── Eventos del auxiliar ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Long> listarEventosAuxiliar(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "AUXILIAR");

        return auxiliarEventoRepository.findByIdAuxiliar(idUsuario)
            .stream()
            .map(ae -> ae.getEvento().getIdEvento())
            .toList();
    }

    @Transactional
    public void actualizarEventosAuxiliar(Long idUsuario, List<Long> eventoIds) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "AUXILIAR");

        Set<Long> nuevos = new HashSet<>(eventoIds);
        List<AuxiliarEvento> actuales = auxiliarEventoRepository.findByIdAuxiliar(idUsuario);
        Set<Long> actualesIds = actuales.stream()
            .map(ae -> ae.getEvento().getIdEvento())
            .collect(Collectors.toSet());

        for (AuxiliarEvento actual : actuales) {
            if (!nuevos.contains(actual.getEvento().getIdEvento())) {
                AuxiliarEventoId pk = new AuxiliarEventoId(idUsuario, actual.getEvento().getIdEvento());
                auxiliarEventoRepository.deleteById(pk);
            }
        }

        for (Long idEvento : nuevos) {
            if (!actualesIds.contains(idEvento)) {
                Evento evento = eventoRepository.findById(idEvento)
                    .orElseThrow(() -> new ResourceNotFoundException("Evento", idEvento));
                AuxiliarEvento asignacion = new AuxiliarEvento(usuario, evento);
                auxiliarEventoRepository.save(asignacion);
            }
        }
    }

    // ── Paralelos del docente ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ParaleloRefRequest> listarParalelosDocente(Long idUsuario) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "DOCENTE");

        return paraleloRepository.findByDocente_IdUsuario(idUsuario)
            .stream()
            .map(paralelo -> {
                ParaleloRefRequest dto = new ParaleloRefRequest();
                dto.setIdCurso(paralelo.getId().getIdCurso());
                dto.setCodigo(paralelo.getId().getCodigo());
                return dto;
            })
            .toList();
    }

    @Transactional
    public void actualizarParalelosDocente(Long idUsuario, List<ParaleloRefRequest> paralelos) {
        Usuario usuario = buscarUsuario(idUsuario);
        requireRole(usuario, "DOCENTE");

        Set<String> nuevos = paralelos.stream()
            .map(ref -> ref.getIdCurso() + ":" + ref.getCodigo())
            .collect(Collectors.toSet());

        List<Paralelo> actuales = paraleloRepository.findByDocente_IdUsuario(idUsuario);
        for (Paralelo actual : actuales) {
            String key = actual.getId().getIdCurso() + ":" + actual.getId().getCodigo();
            if (!nuevos.contains(key)) {
                actual.setDocente(null);
                paraleloRepository.save(actual);
            }
        }

        for (ParaleloRefRequest ref : paralelos) {
            ParaleloId pk = new ParaleloId(ref.getIdCurso(), ref.getCodigo());
            Paralelo paralelo = paraleloRepository.findById(pk)
                .orElseThrow(() -> new BusinessException(
                    "Paralelo no encontrado: " + ref.getIdCurso() + "-" + ref.getCodigo(), 404));
            paralelo.setDocente(usuario);
            paraleloRepository.save(paralelo);
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private Usuario buscarUsuario(Long idUsuario) {
        return usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", idUsuario));
    }

    private Long getUsuarioActualId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return null;
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getIdUsuario();
    }

    private Long requireUsuarioActualId() {
        Long idUsuario = getUsuarioActualId();
        if (idUsuario == null) {
            throw new BusinessException("Usuario no autenticado", 401);
        }
        return idUsuario;
    }

    private void validarExterno(Usuario usuario) {
        Participante participante = participanteRepository.findById(usuario.getIdUsuario())
            .orElse(null);

        boolean esExterno = participante != null
            && participante.getTipoParticipante() == Participante.TipoParticipante.EXTERNO;

        if (!esExterno) {
            throw new BusinessException("Solo usuarios externos pueden modificar estos datos", 403);
        }
    }

    private void requireRole(Usuario usuario, String nombreRol) {
        String rolNormalizado = normalizeRolName(nombreRol);
        boolean hasRole = usuario.getRoles().stream()
            .anyMatch(r -> normalizeRolName(r.getNombre()).equals(rolNormalizado));
        if (!hasRole) {
            throw new BusinessException(
                "El usuario no tiene el rol " + nombreRol, 400);
        }
    }

    private String normalizeRolName(String nombreRol) {
        if (nombreRol == null) return "";
        return nombreRol.replace("Ñ", "N").replace("ñ", "n").toUpperCase();
    }

    private java.util.Optional<Rol> findRolByNombreCompat(String nombreRol) {
        if (nombreRol == null) return java.util.Optional.empty();

        java.util.Optional<Rol> direct = rolRepository.findByNombre(nombreRol);
        if (direct.isPresent()) return direct;

        String normalizado = normalizeRolName(nombreRol);
        if ("DISENADOR".equals(normalizado)) {
            return rolRepository.findByNombre("DISEÑADOR")
                .or(() -> rolRepository.findByNombre("DISENADOR"));
        }

        return java.util.Optional.empty();
    }

    // Convierte Usuario → UsuarioResumenDto manualmente
    // (ModelMapper no resuelve bien la lista de roles porque es Set<Rol>)
    private UsuarioResumenDto toResumenDto(Usuario u) {
        UsuarioResumenDto dto = new UsuarioResumenDto();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setUsername(u.getUsername());
        dto.setCi(u.getCi());
        dto.setNombres(u.getNombres());
        dto.setApellidos(u.getApellidos());
        dto.setEmail(u.getEmail());
        dto.setTipoUsuario(u.getPasswordHash() == null ? "INTERNO" : "EXTERNO");
        dto.setEmailVerificado(u.isEmailVerificado());
        dto.setEstado(u.getEstado().name());
        dto.setFechaRegistro(u.getFechaRegistro());
        dto.setRoles(u.getRoles().stream()
            .map(Rol::getNombre)
            .map(this::normalizeRolName)
            .toList());
        return dto;
    }

    private UsuarioDetalleDto toDetalleDto(Usuario u) {
        UsuarioDetalleDto dto = new UsuarioDetalleDto();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setUsername(u.getUsername());
        dto.setCi(u.getCi());
        dto.setNombres(u.getNombres());
        dto.setApellidos(u.getApellidos());
        dto.setEmail(u.getEmail());
        dto.setTipoUsuario(u.getPasswordHash() == null ? "INTERNO" : "EXTERNO");
        dto.setEmailVerificado(u.isEmailVerificado());
        dto.setEstado(u.getEstado().name());
        dto.setFechaRegistro(u.getFechaRegistro());
        dto.setRoles(u.getRoles().stream()
            .map(r -> modelMapper.map(r, RolDto.class))
            .toList());

        // Perfil docente (si tiene el rol)
        docenteRepository.findById(u.getIdUsuario())
            .ifPresent(d -> dto.setTitulo(d.getTitulo()));

        // Perfil participante (si tiene el rol)
        participanteRepository.findById(u.getIdUsuario())
            .ifPresent(p -> dto.setTipoParticipante(p.getTipoParticipante().name()));

        return dto;
    }
}