package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.ParaleloRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.EmitirCertificadoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.AnularCertificadoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.Certificado;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository.CertificadoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.service.CertificadoService;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.ConfirmarNotasRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.EvaluacionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.ModificarNotaRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.RegistrarNotaRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.SolicitudEventoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.SolicitudEmisionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.EvaluacionEstudiante;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Historial;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.SolicitudEmision;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.AsistenciaRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.EvaluacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.HistorialRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.SolicitudEmisionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Asistencia;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluacionService {

    private final EvaluacionRepository      evaluacionRepository;
    private final HistorialRepository       historialRepository;
    private final AsistenciaRepository      asistenciaRepository;
    private final SolicitudEmisionRepository solicitudRepository;
    private final InscripcionRepository     inscripcionRepository;
    private final ParaleloRepository        paraleloRepository;
    private final CertificadoRepository     certificadoRepository;
    private final CertificadoService        certificadoService;
    private final EventoRepository          eventoRepository;
    private final UsuarioRepository         usuarioRepository;
    

    // ── Registrar nota a un inscrito ─────────────────────────────────────────
    
    @Transactional
    public EvaluacionDto registrarNota(RegistrarNotaRequest request) {
        Inscripcion inscripcion = buscarInscripcion(request.getIdInscripcion());

        if (inscripcion.getCurso() == null) {
            throw new BusinessException(
                "Solo se pueden registrar notas en inscripciones a cursos", 400);
        }
        if (inscripcion.getEstado() != Inscripcion.EstadoInscripcion.CONFIRMADA) {
            throw new BusinessException(
                "Solo se pueden registrar notas en inscripciones confirmadas", 400);
        }

        Usuario docente = getUsuarioActual();
        verificarDocenteDelParalelo(docente, inscripcion);

        // Verificar que el paralelo no haya sido confirmado ya
        // (si ya existe una SolicitudEmision para este paralelo, las notas quedan bloqueadas)
        if (inscripcion.getCodigoParalelo() != null) {
            boolean paraleoConfirmado = solicitudRepository
                .existsByCurso_IdCursoAndCodigoParalelo(
                    inscripcion.getCurso().getIdCurso(),
                    inscripcion.getCodigoParalelo()
                );
            if (paraleoConfirmado) {
                throw new BusinessException(
                    "Las notas de este paralelo ya fueron confirmadas y no pueden modificarse. "
                    + "Contacta al administrador para correcciones.", 403);
            }
        }

        Curso curso = inscripcion.getCurso();
        EvaluacionEstudiante.EstadoEvaluacion estado =
            request.getNotaFinal().compareTo(curso.getNotaAprobacion()) >= 0
                ? EvaluacionEstudiante.EstadoEvaluacion.APROBADO
                : EvaluacionEstudiante.EstadoEvaluacion.REPROBADO;

        // Buscar evaluación existente o crear una nueva
        EvaluacionEstudiante evaluacion = evaluacionRepository
            .findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion())
            .orElse(EvaluacionEstudiante.builder()
                .inscripcion(inscripcion)
                .build());

        evaluacion.setNotaFinal(request.getNotaFinal());
        evaluacion.setEstado(estado);
        evaluacion = evaluacionRepository.save(evaluacion);

        log.info("Nota registrada/actualizada — inscripción: {} — nota: {} — estado: {}",
            inscripcion.getIdInscripcion(), request.getNotaFinal(), estado);

        return toEvaluacionDto(evaluacion);
    }
        
    // ── Registrar notas en lote para un paralelo ─────────────────────────────
    @Transactional
    public List<EvaluacionDto> registrarNotasLote(List<RegistrarNotaRequest> requests) {
        return requests.stream()
            .map(this::registrarNota)
            .toList();
    }

    // ── Ver notas de un paralelo ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<EvaluacionDto> notasDeParalelo(Long idCurso, String codigo) {
        return evaluacionRepository.findByParalelo(idCurso, codigo)
            .stream()
            .map(this::toEvaluacionDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean paraleloConfirmado(Long idCurso, String codigo) {
        return solicitudRepository
            .existsByCurso_IdCursoAndCodigoParalelo(
                idCurso,
                codigo
            );
    }

    // ── Modificar nota (solo administrador) ──────────────────────────────────
    @Transactional
    public EvaluacionDto modificarNota(Long idEvaluacion, ModificarNotaRequest request) {
        EvaluacionEstudiante evaluacion = evaluacionRepository.findById(idEvaluacion)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Evaluacion", idEvaluacion));

        Usuario admin = getUsuarioActual();

        // Guardar historial del cambio antes de modificar
        Historial historial = Historial.builder()
            .evaluacion(evaluacion)
            .notaAnterior(evaluacion.getNotaFinal())
            .notaNueva(request.getNotaNueva())
            .motivo(request.getMotivo())
            .cambiadoPor(admin)
            .build();
        historialRepository.save(historial);

        // Recalcular estado con la nota mínima del curso
        Curso curso = evaluacion.getInscripcion().getCurso();
        EvaluacionEstudiante.EstadoEvaluacion nuevoEstado =
            request.getNotaNueva().compareTo(curso.getNotaAprobacion()) >= 0
                ? EvaluacionEstudiante.EstadoEvaluacion.APROBADO
                : EvaluacionEstudiante.EstadoEvaluacion.REPROBADO;

        EvaluacionEstudiante.EstadoEvaluacion estadoAnterior = evaluacion.getEstado();

        evaluacion.setNotaFinal(request.getNotaNueva());
        evaluacion.setEstado(nuevoEstado);
        evaluacionRepository.save(evaluacion);

        if (estadoAnterior == EvaluacionEstudiante.EstadoEvaluacion.APROBADO
                && nuevoEstado == EvaluacionEstudiante.EstadoEvaluacion.REPROBADO) {
            certificadoRepository
                .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                    evaluacion.getInscripcion().getIdInscripcion(),
                    Certificado.EstadoEmision.GENERADO)
                .ifPresent(certificado -> {
                    AnularCertificadoRequest anularRequest = new AnularCertificadoRequest();
                    anularRequest.setMotivo(
                        "Anulado por cambio de nota: " + request.getMotivo());
                    anularRequest.setReemitir(false);
                    certificadoService.anular(certificado.getIdCertificado(), anularRequest);
                });
        }

        if (estadoAnterior == EvaluacionEstudiante.EstadoEvaluacion.REPROBADO
                && nuevoEstado == EvaluacionEstudiante.EstadoEvaluacion.APROBADO) {
            Inscripcion inscripcion = evaluacion.getInscripcion();
            String codigoParalelo = inscripcion.getCodigoParalelo();

            if (codigoParalelo != null) {
                boolean loteEmitido = solicitudRepository
                    .existsByCurso_IdCursoAndCodigoParaleloAndEstado(
                        inscripcion.getCurso().getIdCurso(),
                        codigoParalelo,
                        SolicitudEmision.EstadoSolicitud.COMPLETADO
                    );

                if (loteEmitido) {
                    boolean yaGenerado = certificadoRepository
                        .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                            inscripcion.getIdInscripcion(),
                            Certificado.EstadoEmision.GENERADO)
                        .isPresent();

                    if (!yaGenerado) {
                        EmitirCertificadoRequest emitirRequest = new EmitirCertificadoRequest();
                        emitirRequest.setIdInscripcion(inscripcion.getIdInscripcion());
                        certificadoService.emitir(emitirRequest);
                    }
                }
            }
        }

        log.info("Nota modificada por admin {} — evaluación: {} — nueva nota: {}",
            admin.getUsername(), idEvaluacion, request.getNotaNueva());

        return toEvaluacionDto(evaluacion);
    }

    // ── Ver historial de cambios de una evaluación ───────────────────────────
    @Transactional(readOnly = true)
    public List<Historial> historialDeEvaluacion(Long idEvaluacion) {
        return historialRepository
            .findByEvaluacion_IdEvaluacionOrderByFechaCambioDesc(idEvaluacion);
    }

    // ── Confirmar notas de un paralelo (docente) ─────────────────────────────
    // Genera solicitud de emisión de certificados para el coordinador
    @Transactional
    public SolicitudEmisionDto confirmarNotas(Long idCurso, ConfirmarNotasRequest request) {
        Usuario docente = getUsuarioActual();

        // Verificar que el docente esté asignado al paralelo
        ParaleloId pk = new ParaleloId(idCurso, request.getCodigoParalelo());
        Paralelo paralelo = paraleloRepository.findById(pk)
            .orElseThrow(() -> new BusinessException(
                "Paralelo no encontrado", 404));

        if (paralelo.getDocente() == null ||
                !paralelo.getDocente().getIdUsuario().equals(docente.getIdUsuario())) {
            throw new BusinessException(
                "No estás asignado como docente de este paralelo", 403);
        }

        // Verificar que todos los inscritos confirmados tengan nota
        int sinNota = evaluacionRepository.contarSinNota(
            idCurso, request.getCodigoParalelo());
        if (sinNota > 0) {
            throw new BusinessException(
                "Hay " + sinNota + " participante(s) sin nota registrada. "
                + "Registra todas las notas antes de confirmar.", 400);
        }

        // Contar aprobados
        int aprobados = evaluacionRepository.contarAprobados(
            idCurso, request.getCodigoParalelo());

        // Crear solicitud de emisión
        SolicitudEmision solicitud = SolicitudEmision.builder()
            .curso(paralelo.getCurso())
            .codigoParalelo(request.getCodigoParalelo())
            .docente(docente)
            .cantidadAprobados(aprobados)
            .estado(SolicitudEmision.EstadoSolicitud.PENDIENTE)
            .notas(request.getNotas())
            .build();

        solicitud = solicitudRepository.save(solicitud);
        log.info("Notas confirmadas — paralelo {}/{} — aprobados: {} — solicitud: {}",
            idCurso, request.getCodigoParalelo(), aprobados, solicitud.getIdSolicitud());

        return toSolicitudDto(solicitud);
    }

    // ── Ver solicitudes de emisión pendientes (coordinador) ──────────────────
    @Transactional(readOnly = true)
    public List<SolicitudEmisionDto> solicitudesPendientes() {
        return solicitudRepository
            .findByEstadoOrderByFechaSolicitudAsc(SolicitudEmision.EstadoSolicitud.PENDIENTE)
            .stream()
            .map(this::toSolicitudDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SolicitudEmisionDto> solicitudesTodas() {
        return solicitudRepository
            .findAllByOrderByFechaSolicitudDesc()
            .stream()
            .map(this::toSolicitudDto)
            .toList();
    }

    // ── Cambiar estado de solicitud (coordinador) ────────────────────────────
    @Transactional
    public SolicitudEmisionDto procesarSolicitud(Long idSolicitud, String nuevoEstado) {
        SolicitudEmision solicitud = solicitudRepository.findById(idSolicitud)
            .orElseThrow(() -> new ResourceNotFoundException(
                "SolicitudEmision", idSolicitud));

        Usuario coordinador = getUsuarioActual();

        try {
            solicitud.setEstado(SolicitudEmision.EstadoSolicitud.valueOf(nuevoEstado));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                "Estado inválido. Use: PENDIENTE, EN_PROCESO o COMPLETADO", 400);
        }

        solicitud.setProcesadoPor(coordinador);
        solicitud.setFechaProcesamiento(java.time.LocalDateTime.now());
        solicitudRepository.save(solicitud);

        if (solicitud.getEvento() != null
                && solicitud.getEstado() == SolicitudEmision.EstadoSolicitud.COMPLETADO) {
            emitirCertificadosEvento(solicitud.getEvento().getIdEvento());
        }

        return toSolicitudDto(solicitud);
    }

    // ── Crear solicitud de emisión para evento (coordinador/admin) ─────────
    @Transactional
    public SolicitudEmisionDto solicitarEmisionEvento(
            Long idEvento,
            SolicitudEventoRequest request) {
        Evento evento = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", idEvento));
        Usuario coordinador = getUsuarioActual();

        boolean existePendiente = solicitudRepository
            .existsByEvento_IdEventoAndEstado(
                idEvento, SolicitudEmision.EstadoSolicitud.PENDIENTE);
        boolean existeEnProceso = solicitudRepository
            .existsByEvento_IdEventoAndEstado(
                idEvento, SolicitudEmision.EstadoSolicitud.EN_PROCESO);
        boolean existeCompletada = solicitudRepository
            .existsByEvento_IdEventoAndEstado(
                idEvento, SolicitudEmision.EstadoSolicitud.COMPLETADO);

        if (existePendiente || existeEnProceso || existeCompletada) {
            throw new BusinessException(
                "Ya existe una solicitud de emisión para este evento", 409);
        }

        int asistentes = (int) asistenciaRepository
            .countByInscripcion_Evento_IdEvento(idEvento);

        SolicitudEmision solicitud = SolicitudEmision.builder()
            .evento(evento)
            .docente(coordinador)
            .cantidadAprobados(asistentes)
            .estado(SolicitudEmision.EstadoSolicitud.PENDIENTE)
            .notas(request != null ? request.getNotas() : null)
            .build();

        solicitud = solicitudRepository.save(solicitud);

        log.info("Solicitud de emisión creada para evento {} — asistentes: {} — solicitud: {}",
            idEvento, asistentes, solicitud.getIdSolicitud());

        return toSolicitudDto(solicitud);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void verificarDocenteDelParalelo(Usuario docente, Inscripcion inscripcion) {
        if (inscripcion.getCodigoParalelo() == null) return;

        ParaleloId pk = new ParaleloId(
            inscripcion.getCurso().getIdCurso(),
            inscripcion.getCodigoParalelo()
        );

        paraleloRepository.findById(pk).ifPresent(paralelo -> {
            if (paralelo.getDocente() == null ||
                    !paralelo.getDocente().getIdUsuario().equals(docente.getIdUsuario())) {

                // Admin puede registrar notas en cualquier paralelo
                boolean esAdmin = docente.getRoles().stream()
                    .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));
                if (!esAdmin) {
                    throw new BusinessException(
                        "No estás asignado como docente de este paralelo", 403);
                }
            }
        });
    }

    private Inscripcion buscarInscripcion(Long idInscripcion) {
        return inscripcionRepository.findById(idInscripcion)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inscripcion", idInscripcion));
    }

    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private void emitirCertificadosEvento(Long idEvento) {
        List<Asistencia> asistencias = asistenciaRepository.findByIdEvento(idEvento);

        for (Asistencia asistencia : asistencias) {
            Long idInscripcion = asistencia.getInscripcion().getIdInscripcion();

            boolean yaGenerado = certificadoRepository
                .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                    idInscripcion,
                    Certificado.EstadoEmision.GENERADO)
                .isPresent();

            if (yaGenerado) {
                continue;
            }

            try {
                EmitirCertificadoRequest request = new EmitirCertificadoRequest();
                request.setIdInscripcion(idInscripcion);
                certificadoService.emitir(request);
            } catch (BusinessException ex) {
                log.warn("No se pudo emitir certificado de evento para inscripción {}: {}",
                    idInscripcion, ex.getMessage());
            }
        }
    }

    private EvaluacionDto toEvaluacionDto(EvaluacionEstudiante e) {
        EvaluacionDto dto = new EvaluacionDto();
        dto.setIdEvaluacion(e.getIdEvaluacion());
        dto.setIdInscripcion(e.getInscripcion().getIdInscripcion());
        dto.setNombreParticipante(
            e.getInscripcion().getParticipante().getNombres()
            + " " + e.getInscripcion().getParticipante().getApellidos());
        dto.setUsername(e.getInscripcion().getParticipante().getUsername());
        dto.setNotaFinal(e.getNotaFinal());
        dto.setEstado(e.getEstado().name());
        dto.setFechaRegistro(e.getFechaRegistro());
        return dto;
    }

    private SolicitudEmisionDto toSolicitudDto(SolicitudEmision s) {
        SolicitudEmisionDto dto = new SolicitudEmisionDto();
        dto.setIdSolicitud(s.getIdSolicitud());
        dto.setCodigoParalelo(s.getCodigoParalelo());
        if (s.getDocente() != null) {
            dto.setNombreDocente(
                s.getDocente().getNombres() + " " + s.getDocente().getApellidos());
        }
        dto.setCantidadAprobados(s.getCantidadAprobados());
        dto.setEstado(s.getEstado().name());
        dto.setNotas(s.getNotas());
        dto.setFechaSolicitud(s.getFechaSolicitud());
        dto.setFechaProcesamiento(s.getFechaProcesamiento());

        if (s.getCurso() != null) {
            dto.setNombreActividad(s.getCurso().getNombre());
        } else if (s.getEvento() != null) {
            dto.setNombreActividad(s.getEvento().getNombre());
        }
        return dto;
    }
}
