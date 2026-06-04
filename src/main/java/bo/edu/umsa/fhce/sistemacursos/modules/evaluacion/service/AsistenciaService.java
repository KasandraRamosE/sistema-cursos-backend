// src/main/java/.../modules/evaluacion/service/AsistenciaService.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.service;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.AnularCertificadoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.EmitirCertificadoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.Certificado;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository.CertificadoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.service.CertificadoService;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.AsistenciaAdminDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.AsistenciaDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.RegistrarAsistenciaRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Asistencia;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.AsistenciaRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.SolicitudEmisionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.AuxiliarEventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsistenciaService {

    private final AsistenciaRepository    asistenciaRepository;
    private final InscripcionRepository   inscripcionRepository;
    private final AuxiliarEventoRepository auxiliarEventoRepository;
    private final SolicitudEmisionRepository solicitudRepository;
    private final CertificadoRepository  certificadoRepository;
    private final CertificadoService     certificadoService;
    private final UsuarioRepository       usuarioRepository;

    // ── Registrar asistencia ─────────────────────────────────────────────────
    @Transactional
    public AsistenciaDto registrar(RegistrarAsistenciaRequest request) {
        Inscripcion inscripcion = inscripcionRepository
            .findById(request.getIdInscripcion())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inscripcion", request.getIdInscripcion()));

        // Solo inscripciones a eventos tienen asistencia
        if (inscripcion.getEvento() == null) {
            throw new BusinessException(
                "Solo se registra asistencia en inscripciones a eventos", 400);
        }

        // La inscripción debe estar confirmada
        if (inscripcion.getEstado() != Inscripcion.EstadoInscripcion.CONFIRMADA) {
            throw new BusinessException(
                "Solo se puede registrar asistencia en inscripciones confirmadas", 400);
        }

        // Verificar que no esté ya registrada
        if (asistenciaRepository.existsByInscripcion_IdInscripcion(
                inscripcion.getIdInscripcion())) {
            throw new BusinessException(
                "La asistencia de este participante ya fue registrada", 409);
        }

        // Verificar que el usuario tenga permisos para este evento
        // (coordinador de la carrera o auxiliar asignado)
        Usuario registrador = getUsuarioActual();
        verificarPermisoRegistro(registrador, inscripcion);

        Asistencia asistencia = Asistencia.builder()
            .inscripcion(inscripcion)
            .registradoPor(registrador)
            .build();

        asistencia = asistenciaRepository.save(asistencia);
        log.info("Asistencia registrada — inscripción: {} — registrado por: {}",
            inscripcion.getIdInscripcion(), registrador.getUsername());

        actualizarCertificadoPorAsistencia(inscripcion, true);

        return toAsistenciaDto(asistencia);
    }

    // ── Ver asistentes de un evento ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<AsistenciaDto> asistentesDeEvento(Long idEvento) {
        return asistenciaRepository.findByIdEvento(idEvento)
            .stream()
            .map(this::toAsistenciaDto)
            .toList();
    }

    // ── Ver inscripciones de un evento con asistencia ─────────────────────
    @Transactional(readOnly = true)
    public List<AsistenciaAdminDto> inscripcionesConAsistencia(Long idEvento) {
        return inscripcionRepository.findByEvento_IdEvento(idEvento)
            .stream()
            .map(inscripcion -> {
                AsistenciaAdminDto dto = new AsistenciaAdminDto();
                dto.setIdInscripcion(inscripcion.getIdInscripcion());
                dto.setNombreParticipante(
                    inscripcion.getParticipante().getNombres()
                        + " " + inscripcion.getParticipante().getApellidos());
                dto.setUsername(inscripcion.getParticipante().getUsername());
                dto.setEmail(inscripcion.getParticipante().getEmail());
                dto.setEstadoInscripcion(inscripcion.getEstado().name());

                asistenciaRepository.findByInscripcion_IdInscripcion(
                    inscripcion.getIdInscripcion())
                    .ifPresentOrElse(asistencia -> {
                        dto.setIdAsistencia(asistencia.getIdAsistencia());
                        dto.setAsistio(true);
                        dto.setRegistradoPor(
                            asistencia.getRegistradoPor().getNombres()
                                + " " + asistencia.getRegistradoPor().getApellidos());
                        dto.setFechaRegistro(asistencia.getFechaRegistro());
                    }, () -> dto.setAsistio(false));

                return dto;
            })
            .toList();
    }

    // ── Anular asistencia (solo coordinador o admin) ─────────────────────────
    @Transactional
    public void anular(Long idInscripcion) {
        Asistencia asistencia = asistenciaRepository
            .findByInscripcion_IdInscripcion(idInscripcion)
            .orElseThrow(() -> new BusinessException(
                "No existe registro de asistencia para esta inscripción", 404));

        Usuario usuario = getUsuarioActual();
        validarPermisoAnulacion(usuario, asistencia);

        asistenciaRepository.delete(asistencia);
        log.info("Asistencia anulada — inscripción: {}", idInscripcion);

        actualizarCertificadoPorAsistencia(asistencia.getInscripcion(), false);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void verificarPermisoRegistro(Usuario usuario, Inscripcion inscripcion) {
        boolean esAdmin = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));
        if (esAdmin) return;

        boolean esCoordinador = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("COORDINADOR"));
        if (esCoordinador) return;

        // Verificar que sea auxiliar asignado a ese evento
        boolean esAuxiliarAsignado = auxiliarEventoRepository
            .existsByAuxiliar_IdUsuarioAndEvento_IdEvento(
                usuario.getIdUsuario(),
                inscripcion.getEvento().getIdEvento()
            );

        if (!esAuxiliarAsignado) {
            throw new BusinessException(
                "No tienes permisos para registrar asistencia en este evento", 403);
        }
    }

    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private void validarPermisoAnulacion(Usuario usuario, Asistencia asistencia) {
        boolean esAdmin = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));
        boolean esCoordinador = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("COORDINADOR"));

        if (esAdmin || esCoordinador) {
            return;
        }

        boolean esAuxiliar = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("AUXILIAR"));

        if (!esAuxiliar) {
            throw new BusinessException(
                "No tienes permisos para anular asistencias", 403);
        }

        boolean esAuxiliarAsignado = auxiliarEventoRepository
            .existsByAuxiliar_IdUsuarioAndEvento_IdEvento(
                usuario.getIdUsuario(),
                asistencia.getInscripcion().getEvento().getIdEvento()
            );

        if (!esAuxiliarAsignado) {
            throw new BusinessException(
                "No tienes permisos para anular asistencias en este evento", 403);
        }

        LocalDateTime ahora = LocalDateTime.now();
        Duration transcurrido = Duration.between(asistencia.getFechaRegistro(), ahora);

        if (transcurrido.toMinutes() > 60) {
            throw new BusinessException(
                "Solo puedes anular asistencias dentro de la primera hora", 403);
        }
    }

    private void actualizarCertificadoPorAsistencia(Inscripcion inscripcion, boolean asistio) {
        if (inscripcion.getEvento() == null) {
            return;
        }

        if (!asistio) {
            certificadoRepository
                .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                    inscripcion.getIdInscripcion(),
                    Certificado.EstadoEmision.GENERADO)
                .ifPresent(certificado -> {
                    AnularCertificadoRequest request = new AnularCertificadoRequest();
                    request.setMotivo("Auto-anulado por inasistencia");
                    request.setReemitir(false);
                    certificadoService.anular(certificado.getIdCertificado(), request);
                });
            return;
        }

        boolean loteEmitido = solicitudRepository.existsByEvento_IdEventoAndEstado(
            inscripcion.getEvento().getIdEvento(),
            bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.SolicitudEmision.EstadoSolicitud.COMPLETADO
        );

        if (!loteEmitido) {
            return;
        }

        boolean yaGenerado = certificadoRepository
            .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                inscripcion.getIdInscripcion(),
                Certificado.EstadoEmision.GENERADO)
            .isPresent();

        if (yaGenerado) {
            return;
        }

        EmitirCertificadoRequest request = new EmitirCertificadoRequest();
        request.setIdInscripcion(inscripcion.getIdInscripcion());
        certificadoService.emitir(request);
    }

    private AsistenciaDto toAsistenciaDto(Asistencia a) {
        AsistenciaDto dto = new AsistenciaDto();
        dto.setIdAsistencia(a.getIdAsistencia());
        dto.setIdInscripcion(a.getInscripcion().getIdInscripcion());
        dto.setNombreParticipante(
            a.getInscripcion().getParticipante().getNombres()
            + " " + a.getInscripcion().getParticipante().getApellidos());
        dto.setRegistradoPor(
            a.getRegistradoPor().getNombres()
            + " " + a.getRegistradoPor().getApellidos());
        dto.setFechaRegistro(a.getFechaRegistro());
        return dto;
    }
}
