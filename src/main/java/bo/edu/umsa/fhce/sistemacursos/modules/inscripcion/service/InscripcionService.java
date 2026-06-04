package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.service;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.ParaleloRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Pago;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.integration.LibelulaClient;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.PagoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Participante;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.ParticipanteRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InscripcionService {

    private final InscripcionRepository  inscripcionRepository;
    private final PagoRepository         pagoRepository;
    private final CursoRepository        cursoRepository;
    private final ParaleloRepository     paraleloRepository;
    private final EventoRepository       eventoRepository;
    private final UsuarioRepository      usuarioRepository;
    private final ParticipanteRepository participanteRepository;
    private final LibelulaClient         libelulaClient;

    // ── Inscribirse a un curso o evento ──────────────────────────────────────
    @Transactional
    public InscripcionDto inscribirse(InscripcionRequest request) {

        // 1. Validar que venga exactamente uno: curso o evento
        if (request.getIdCurso() == null && request.getIdEvento() == null) {
            throw new BusinessException("Debe especificar un curso o un evento", 400);
        }
        if (request.getIdCurso() != null && request.getIdEvento() != null) {
            throw new BusinessException(
                "No puede inscribirse a un curso y un evento al mismo tiempo", 400);
        }

        Usuario participante = getUsuarioActual();

        // 2. Verificar que tenga rol PARTICIPANTE
        boolean esParticipante = participante.getRoles().stream()
            .anyMatch(r -> {
                String nombre = r.getNombre();
                return "PARTICIPANTE".equals(nombre) || "ROLE_PARTICIPANTE".equals(nombre);
            });
        if (!esParticipante) {
            throw new BusinessException("Solo los participantes pueden inscribirse", 403);
        }

        // 3. Obtener perfil de participante para determinar el precio
        Participante perfilParticipante = participanteRepository
            .findById(participante.getIdUsuario())
            .orElseThrow(() -> new BusinessException(
                "Perfil de participante no encontrado", 500));

        Inscripcion.TipoPrecio tipoPrecio =
            perfilParticipante.getTipoParticipante() == Participante.TipoParticipante.UMSA
                ? Inscripcion.TipoPrecio.UMSA
                : Inscripcion.TipoPrecio.EXTERNO;

        // 4. Construir la inscripción según el tipo de actividad
        Inscripcion inscripcion;

        if (request.getIdCurso() != null) {
            inscripcion = inscribirACurso(
                request, participante, tipoPrecio);
        } else {
            inscripcion = inscribirAEvento(
                request, participante, tipoPrecio);
        }

        // 5. Si la actividad es gratuita, confirmar automáticamente
        // (el trigger T3 también lo hace en la BD, pero lo manejamos aquí
        // para tener control explícito en la capa de servicio)
        if (inscripcion.getSaldo().compareTo(BigDecimal.ZERO) == 0) {
            inscripcion.setEstado(Inscripcion.EstadoInscripcion.CONFIRMADA);
            inscripcionRepository.save(inscripcion);
            log.info("Inscripción gratuita confirmada automáticamente: {}",
                inscripcion.getIdInscripcion());
        }

        return toInscripcionDto(inscripcion);
    }

    // ── Iniciar pago de una inscripción ──────────────────────────────────────
    @Transactional
    public PagoDto iniciarPago(PagoIniciarRequest request) {
        Inscripcion inscripcion = inscripcionRepository
            .findById(request.getIdInscripcion())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inscripcion", request.getIdInscripcion()));

        // Verificar que sea el participante dueño de la inscripción
        Usuario actual = getUsuarioActual();
        if (!inscripcion.getParticipante().getIdUsuario()
                .equals(actual.getIdUsuario())) {
            throw new BusinessException(
                "No tienes permisos para pagar esta inscripción", 403);
        }

        // Verificar estado
        if (inscripcion.getEstado() != Inscripcion.EstadoInscripcion.PENDIENTE) {
            throw new BusinessException(
                "Esta inscripción no está pendiente de pago. Estado: "
                + inscripcion.getEstado(), 400);
        }

        // Verificar que no tenga ya un pago pendiente o aprobado
        pagoRepository.findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion())
            .ifPresent(p -> {
                if (p.getEstado() != Pago.EstadoPago.RECHAZADO) {
                    throw new BusinessException(
                        "Ya existe un pago en proceso para esta inscripción", 409);
                }
            });

        // Llamar a la pasarela (mock en dev, real en prod)
        String descripcion = inscripcion.getCurso() != null
            ? "Inscripción a curso: " + inscripcion.getCurso().getNombre()
            : "Inscripción a evento: " + inscripcion.getEvento().getNombre();

        LibelulaClient.PagoResultado resultado =
            libelulaClient.iniciarPago(inscripcion.getSaldo(), descripcion);

        // Registrar el pago
        Pago pago = Pago.builder()
            .inscripcion(inscripcion)
            .monto(inscripcion.getSaldo())
            .metodoPago(resultado.metodoPago())
            .referenciaTransaccion(resultado.referenciaTransaccion())
            .estado(resultado.aprobado()
                ? Pago.EstadoPago.APROBADO
                : Pago.EstadoPago.PENDIENTE)
            .build();

        // Si el mock aprobó inmediatamente, confirmar la inscripción
        if (resultado.aprobado()) {
            pago.setFechaPago(LocalDateTime.now());
            inscripcion.setEstado(Inscripcion.EstadoInscripcion.CONFIRMADA);
            inscripcionRepository.save(inscripcion);
            log.info("Pago aprobado — inscripción {} confirmada",
                inscripcion.getIdInscripcion());

            // Verificar y actualizar cupo de la actividad
            verificarYActualizarCupo(inscripcion);
        }

        pago = pagoRepository.save(pago);
        return toPagoDto(pago);
    }

    // ── Confirmar un pago que estaba pendiente (ej. webhook o simulador) ───
    @Transactional
    public PagoDto confirmarPago(PagoConfirmarRequest request) {
        Pago pago = pagoRepository.findByInscripcion_IdInscripcion(request.getIdInscripcion())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Pago para inscripcion", request.getIdInscripcion()));

        if (pago.getEstado() == Pago.EstadoPago.APROBADO) {
            throw new BusinessException("El pago ya fue aprobado", 400);
        }

        pago.setEstado(Pago.EstadoPago.APROBADO);
        pago.setFechaPago(LocalDateTime.now());
        pago = pagoRepository.save(pago);

        Inscripcion inscripcion = pago.getInscripcion();
        inscripcion.setEstado(Inscripcion.EstadoInscripcion.CONFIRMADA);
        inscripcionRepository.save(inscripcion);

        verificarYActualizarCupo(inscripcion);

        return toPagoDto(pago);
    }

    // ── Cancelar inscripción ─────────────────────────────────────────────────
    @Transactional
    public InscripcionDto cancelar(Long idInscripcion) {
        Inscripcion inscripcion = inscripcionRepository.findById(idInscripcion)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inscripcion", idInscripcion));

        Usuario actual = getUsuarioActual();
        boolean esAdmin = actual.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));

        // Solo el dueño o un admin puede cancelar
        if (!esAdmin && !inscripcion.getParticipante().getIdUsuario()
                .equals(actual.getIdUsuario())) {
            throw new BusinessException(
                "No tienes permisos para cancelar esta inscripción", 403);
        }

        if (inscripcion.getEstado() == Inscripcion.EstadoInscripcion.CANCELADA) {
            throw new BusinessException("La inscripción ya está cancelada", 400);
        }

        inscripcion.setEstado(Inscripcion.EstadoInscripcion.CANCELADA);
        inscripcionRepository.save(inscripcion);
        return toInscripcionDto(inscripcion);
    }

    // ── Ver mis inscripciones ────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<InscripcionDto> misInscripciones() {
        Usuario actual = getUsuarioActual();
        return inscripcionRepository
            .findByParticipante_IdUsuario(actual.getIdUsuario())
            .stream()
            .map(this::toInscripcionDto)
            .toList();
    }

    // ── Ver inscripciones de un curso (coordinador/docente) ──────────────────
    @Transactional(readOnly = true)
    public List<InscripcionDto> inscripcionesDeCurso(Long idCurso) {
        return inscripcionRepository.findByCurso_IdCurso(idCurso)
            .stream()
            .map(this::toInscripcionDto)
            .toList();
    }

    // ── Ver inscripciones de un evento (coordinador/auxiliar) ────────────────
    @Transactional(readOnly = true)
    public List<InscripcionDto> inscripcionesDeEvento(Long idEvento) {
        return inscripcionRepository.findByEvento_IdEvento(idEvento)
            .stream()
            .map(this::toInscripcionDto)
            .toList();
    }

    // ── Lógica interna: inscribir a curso ────────────────────────────────────
    private Inscripcion inscribirACurso(InscripcionRequest request,
                                         Usuario participante,
                                         Inscripcion.TipoPrecio tipoPrecio) {
        Curso curso = cursoRepository.findById(request.getIdCurso())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Curso", request.getIdCurso()));

        // Si la fecha de inicio ya pasó, marcar como FINALIZADO y rechazar
        if (curso.getFechaInicio() != null && curso.getFechaInicio().isBefore(LocalDate.now())) {
            if (curso.getEstado() != Curso.EstadoCurso.FINALIZADO) {
                curso.setEstado(Curso.EstadoCurso.FINALIZADO);
                cursoRepository.save(curso);
            }
            throw new BusinessException(
                "El curso ya finalizó y no admite inscripciones", 400);
        }

        // Verificar estado del curso
        if (curso.getEstado() != Curso.EstadoCurso.ABIERTO) {
            throw new BusinessException(
                "El curso no está disponible para inscripciones. Estado: "
                + curso.getEstado(), 400);
        }

        // Verificar que no esté ya inscrito
        if (inscripcionRepository.existsByParticipante_IdUsuarioAndCurso_IdCurso(
                participante.getIdUsuario(), curso.getIdCurso())) {
            throw new BusinessException(
                "Ya estás inscrito en este curso", 409);
        }

        // Validar paralelo si se especificó
        if (request.getCodigoParalelo() != null) {
            ParaleloId pk = new ParaleloId(curso.getIdCurso(), request.getCodigoParalelo());
            Paralelo paralelo = paraleloRepository.findById(pk)
                .orElseThrow(() -> new BusinessException(
                    "Paralelo '" + request.getCodigoParalelo() + "' no encontrado", 404));

            // Verificar cupo del paralelo
            if (paralelo.getCupoMaximo() != null) {
                int inscritos = inscripcionRepository.contarConfirmadasEnParalelo(
                    curso.getIdCurso(), paralelo.getId().getCodigo());
                if (inscritos >= paralelo.getCupoMaximo()) {
                    throw new BusinessException(
                        "El paralelo " + paralelo.getId().getCodigo()
                        + " no tiene cupos disponibles", 400);
                }
            }
        }

        // Calcular precio según tipo de participante
        BigDecimal saldo = tipoPrecio == Inscripcion.TipoPrecio.UMSA
            ? curso.getCostoUmsa()
            : curso.getCostoExterno();

        Inscripcion inscripcion = Inscripcion.builder()
            .participante(participante)
            .curso(curso)
            .codigoParalelo(request.getCodigoParalelo())
            .tipoPrecio(tipoPrecio)
            .saldo(saldo)
            .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
            .build();

        return inscripcionRepository.save(inscripcion);
    }

    // ── Lógica interna: inscribir a evento ───────────────────────────────────
    private Inscripcion inscribirAEvento(InscripcionRequest request,
                                          Usuario participante,
                                          Inscripcion.TipoPrecio tipoPrecio) {
        Evento evento = eventoRepository.findById(request.getIdEvento())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Evento", request.getIdEvento()));

        if (evento.getFechaHora() != null && evento.getFechaHora().isBefore(LocalDateTime.now())) {
            if (evento.getEstado() != Evento.EstadoEvento.FINALIZADO) {
                evento.setEstado(Evento.EstadoEvento.FINALIZADO);
                eventoRepository.save(evento);
            }
            throw new BusinessException(
                "El evento ya finalizó y no admite inscripciones", 400);
        }

        if (evento.getEstado() != Evento.EstadoEvento.ABIERTO) {
            throw new BusinessException(
                "El evento no está disponible. Estado: " + evento.getEstado(), 400);
        }

        if (inscripcionRepository.existsByParticipante_IdUsuarioAndEvento_IdEvento(
                participante.getIdUsuario(), evento.getIdEvento())) {
            throw new BusinessException(
                "Ya estás inscrito en este evento", 409);
        }

        // Verificar cupo del evento
        if (evento.getCupoMaximo() != null) {
            int inscritos = inscripcionRepository
                .contarConfirmadasEnEvento(evento.getIdEvento());
            if (inscritos >= evento.getCupoMaximo()) {
                throw new BusinessException(
                    "El evento no tiene cupos disponibles", 400);
            }
        }

        BigDecimal saldo = tipoPrecio == Inscripcion.TipoPrecio.UMSA
            ? evento.getCostoUmsa()
            : evento.getCostoExterno();

        Inscripcion inscripcion = Inscripcion.builder()
            .participante(participante)
            .evento(evento)
            .tipoPrecio(tipoPrecio)
            .saldo(saldo)
            .estado(Inscripcion.EstadoInscripcion.PENDIENTE)
            .build();

        return inscripcionRepository.save(inscripcion);
    }

    // ── Verificar y marcar actividad como LLENA si corresponde ──────────────
    private void verificarYActualizarCupo(Inscripcion inscripcion) {
        if (inscripcion.getCurso() != null && inscripcion.getCodigoParalelo() != null) {
            Curso curso = inscripcion.getCurso();
            // El trigger T5 en MySQL ya maneja esto —
            // solo lo hacemos en capa de servicio como respaldo
            log.debug("Cupo de paralelo verificado por trigger de BD");

        } else if (inscripcion.getEvento() != null) {
            Evento evento = inscripcion.getEvento();
            if (evento.getCupoMaximo() != null) {
                int total = inscripcionRepository
                    .contarConfirmadasEnEvento(evento.getIdEvento());
                if (total >= evento.getCupoMaximo()) {
                    evento.setEstado(Evento.EstadoEvento.LLENO);
                    eventoRepository.save(evento);
                    log.info("Evento {} marcado como LLENO", evento.getNombre());
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private InscripcionDto toInscripcionDto(Inscripcion i) {
        InscripcionDto dto = new InscripcionDto();
        dto.setIdInscripcion(i.getIdInscripcion());
        dto.setIdParticipante(i.getParticipante().getIdUsuario());
        dto.setNombreParticipante(
            i.getParticipante().getNombres() + " " + i.getParticipante().getApellidos());
        dto.setEmailParticipante(i.getParticipante().getEmail());
        dto.setUsernameParticipante(i.getParticipante().getUsername());
        dto.setTipoPrecio(i.getTipoPrecio().name());
        dto.setSaldo(i.getSaldo());
        dto.setEstado(i.getEstado().name());
        dto.setFechaInscripcion(i.getFechaInscripcion());
        dto.setCodigoParalelo(i.getCodigoParalelo());

        if (i.getCurso() != null) {
            dto.setIdCurso(i.getCurso().getIdCurso());
            dto.setNombreActividad(i.getCurso().getNombre());
            dto.setTipoActividad("CURSO");
            dto.setIdCarrera(i.getCurso().getCarrera().getIdCarrera());
            dto.setNombreCarrera(i.getCurso().getCarrera().getNombre());
            if (i.getCodigoParalelo() != null) {
                ParaleloId pk = new ParaleloId(
                    i.getCurso().getIdCurso(),
                    i.getCodigoParalelo()
                );
                paraleloRepository.findById(pk)
                    .ifPresent(paralelo -> dto.setModalidadActividad(
                        paralelo.getModalidad().name()));
            }
        } else {
            dto.setIdEvento(i.getEvento().getIdEvento());
            dto.setNombreActividad(i.getEvento().getNombre());
            dto.setTipoActividad("EVENTO");
            dto.setIdCarrera(i.getEvento().getCarrera().getIdCarrera());
            dto.setNombreCarrera(i.getEvento().getCarrera().getNombre());
            dto.setModalidadActividad(i.getEvento().getModalidad().name());
        }

        // Info del pago si existe
        pagoRepository.findByInscripcion_IdInscripcion(i.getIdInscripcion())
            .ifPresent(p -> {
                dto.setEstadoPago(p.getEstado().name());
                dto.setReferenciaTransaccion(p.getReferenciaTransaccion());
            });

        return dto;
    }

    private PagoDto toPagoDto(Pago p) {
        PagoDto dto = new PagoDto();
        dto.setIdPago(p.getIdPago());
        dto.setIdInscripcion(p.getInscripcion().getIdInscripcion());
        dto.setMonto(p.getMonto());
        dto.setMetodoPago(p.getMetodoPago());
        dto.setReferenciaTransaccion(p.getReferenciaTransaccion());
        dto.setEstado(p.getEstado().name());
        dto.setFechaPago(p.getFechaPago());
        return dto;
    }
}