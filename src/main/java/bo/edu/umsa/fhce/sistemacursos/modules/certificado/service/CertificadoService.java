// src/main/java/.../modules/certificado/service/CertificadoService.java

package bo.edu.umsa.fhce.sistemacursos.modules.certificado.service;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.CAnulacion;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity.Certificado;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository.AnulacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository.CertificadoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.EvaluacionEstudiante;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.AsistenciaRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.EvaluacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.service.PlantillaService;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificadoService {

    private final CertificadoRepository  certificadoRepository;
    private final AnulacionRepository    anulacionRepository;
    private final InscripcionRepository  inscripcionRepository;
    private final EvaluacionRepository   evaluacionRepository;
    private final AsistenciaRepository   asistenciaRepository;
    private final UsuarioRepository      usuarioRepository;
    private final CertificadoPdfService  pdfService;
    private final PlantillaService plantillaService;

    @Value("${app.email.base-url}")
    private String baseUrl;

    // ── Emitir certificado individual ────────────────────────────────────────
    @Transactional
    public CertificadoDto emitir(EmitirCertificadoRequest request) {
        Inscripcion inscripcion = inscripcionRepository
            .findById(request.getIdInscripcion())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Inscripcion", request.getIdInscripcion()));

        Optional<Certificado> vigente = certificadoRepository
            .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                inscripcion.getIdInscripcion(),
                Certificado.EstadoEmision.GENERADO);

        if (vigente.isPresent()) {
            throw new BusinessException(
                "Este participante ya tiene un certificado emitido", 409);
        }

        int siguienteVersion = certificadoRepository
            .findFirstByInscripcion_IdInscripcionOrderByVersionDescIdCertificadoDesc(
                inscripcion.getIdInscripcion())
            .map(c -> c.getVersion() + 1)
            .orElse(1);

        validarRequisitos(inscripcion);

        // Obtener plantilla vigente de la actividad
        Long idCurso  = inscripcion.getCurso()  != null
            ? inscripcion.getCurso().getIdCurso()   : null;
        Long idEvento = inscripcion.getEvento() != null
            ? inscripcion.getEvento().getIdEvento() : null;

        // obtenerVigente devuelve la entidad — solo necesitamos la ruta del PDF
        String rutaPlantilla = plantillaService
            .obtenerVigente(idCurso, idEvento)
            .getArchivoPdf();

        String codigoVerificacion = UUID.randomUUID().toString();

        Certificado certificado = Certificado.builder()
            .inscripcion(inscripcion)
            .codigoVerificacion(codigoVerificacion)
            .estadoEmision(Certificado.EstadoEmision.GENERADO)
            .version(siguienteVersion)
            .build();

        certificado = certificadoRepository.save(certificado);

        // Generar PDF pasando la ruta de la plantilla
        String rutaPdf = pdfService.generarYGuardar(certificado, rutaPlantilla);
        certificado.setArchivoGenerado(rutaPdf);
        certificado = certificadoRepository.save(certificado);

        log.info("Certificado emitido: {} — inscripción: {}",
            certificado.getIdCertificado(), inscripcion.getIdInscripcion());

        return toCertificadoDto(certificado);
    }
    // ── Emitir en lote para un paralelo ─────────────────────────────────────
    @Transactional
    public List<CertificadoDto> emitirLote(EmitirLoteRequest request) {
        // Obtener inscripciones confirmadas y aprobadas del paralelo
        List<Inscripcion> inscripciones = inscripcionRepository
            .findConfirmadasPorParalelo(request.getIdCurso(), request.getCodigoParalelo());

        if (inscripciones.isEmpty()) {
            throw new BusinessException(
                "No hay inscripciones confirmadas en este paralelo", 400);
        }

        return inscripciones.stream()
            .filter(i -> {
                // Solo emitir para aprobados sin certificado
                boolean aprobado = evaluacionRepository
                    .findByInscripcion_IdInscripcion(i.getIdInscripcion())
                    .map(e -> e.getEstado() == EvaluacionEstudiante.EstadoEvaluacion.APROBADO)
                    .orElse(false);

                boolean sinCertificado = certificadoRepository
                    .findFirstByInscripcion_IdInscripcionAndEstadoEmisionOrderByVersionDescIdCertificadoDesc(
                        i.getIdInscripcion(),
                        Certificado.EstadoEmision.GENERADO)
                    .isEmpty();

                return aprobado && sinCertificado;
            })
            .map(i -> emitir(new EmitirCertificadoRequest() {{
                setIdInscripcion(i.getIdInscripcion());
            }}))
            .toList();
    }

    // ── Anular certificado ───────────────────────────────────────────────────
    @Transactional
    public CertificadoDto anular(Long idCertificado, AnularCertificadoRequest request) {
        Certificado certificado = buscarCertificado(idCertificado);
        Usuario usuario = getUsuarioActual();

        if (certificado.getEstadoEmision() == Certificado.EstadoEmision.REEMITIDO) {
            throw new BusinessException("El certificado ya fue reemitido", 400);
        }

        if (certificado.getEstadoEmision() == Certificado.EstadoEmision.ANULADO
                && !request.isReemitir()) {
            throw new BusinessException("El certificado ya está anulado", 400);
        }

        if (certificado.getEstadoEmision() == Certificado.EstadoEmision.ANULADO) {
            CAnulacion anulacion = anulacionRepository
                .findByCertificado_IdCertificado(certificado.getIdCertificado())
                .orElseThrow(() -> new BusinessException(
                    "No se encontró el registro de anulación del certificado", 400));

            if (anulacion.getCertificadoReemplazo() == null) {
                anulacion.setCertificadoReemplazo(generarReemplazo(certificado));
                anulacionRepository.saveAndFlush(anulacion);
            }

            certificado.setEstadoEmision(Certificado.EstadoEmision.REEMITIDO);
            certificado = certificadoRepository.save(certificado);

            log.info("Certificado {} reemitido por {}",
                idCertificado, usuario.getUsername());

            return toCertificadoDto(certificado);
        }

        // Marcar como anulado
        certificado.setEstadoEmision(request.isReemitir()
            ? Certificado.EstadoEmision.REEMITIDO
            : Certificado.EstadoEmision.ANULADO);
        certificadoRepository.save(certificado);

        Certificado certificadoReemplazo = null;

        // Si se pide reemisión, generar uno nuevo
        if (request.isReemitir()) {
            certificadoReemplazo = generarReemplazo(certificado);
        }

        // Registrar la anulación
        CAnulacion anulacion = CAnulacion.builder()
            .certificado(certificado)
            .usuario(usuario)
            .motivoAnulacion(request.getMotivo())
            .certificadoReemplazo(certificadoReemplazo)
            .build();
        anulacionRepository.saveAndFlush(anulacion);

        // El trigger de anulación puede marcarlo como ANULADO al insertar c_anulacion.
        // Si hay reemplazo, dejamos el estado final correcto dentro de la misma transacción.
        if (request.isReemitir()) {
            certificado.setEstadoEmision(Certificado.EstadoEmision.REEMITIDO);
            certificado = certificadoRepository.save(certificado);
        }

        log.info("Certificado {} anulado por {} — reemitido: {}",
            idCertificado, usuario.getUsername(), request.isReemitir());

        return toCertificadoDto(certificado);
    }

    // ── Descargar certificado (solo el titular) ──────────────────────────────
    @Transactional(readOnly = true)
    public byte[] descargar(Long idCertificado) {
        Certificado certificado = buscarCertificado(idCertificado);
        Usuario actual = getUsuarioActual();

        Long idTitular = certificado.getInscripcion()
            .getParticipante().getIdUsuario();

        boolean esAdmin = actual.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));

        if (!esAdmin && !idTitular.equals(actual.getIdUsuario())) {
            throw new BusinessException(
                "No tienes permisos para descargar este certificado", 403);
        }

        if (certificado.getEstadoEmision() != Certificado.EstadoEmision.GENERADO) {
            throw new BusinessException(
                "Este certificado no está vigente y no puede descargarse", 400);
        }

        // Si el archivo ya existe en disco, usarlo directamente
        if (certificado.getArchivoGenerado() != null) {
            try {
                Path ruta = Paths.get(certificado.getArchivoGenerado());
                if (Files.exists(ruta)) {
                    return Files.readAllBytes(ruta);
                }
            } catch (IOException e) {
                log.warn("Archivo no encontrado en disco, regenerando...");
            }
        }

        // Si no existe, regenerar con la plantilla vigente
        Long idCurso  = certificado.getInscripcion().getCurso()  != null
            ? certificado.getInscripcion().getCurso().getIdCurso()   : null;
        Long idEvento = certificado.getInscripcion().getEvento() != null
            ? certificado.getInscripcion().getEvento().getIdEvento() : null;

        String rutaPlantilla = plantillaService
            .obtenerVigente(idCurso, idEvento)
            .getArchivoPdf();

        return pdfService.generarParaDescarga(certificado, rutaPlantilla);
    }
    // ── Ver mis certificados (participante) ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<CertificadoDto> misCertificados() {
        Usuario actual = getUsuarioActual();
        return certificadoRepository
            .findByInscripcion_Participante_IdUsuario(actual.getIdUsuario())
            .stream()
            .map(this::toCertificadoDto)
            .toList();
    }

    // ── Ver todos los certificados (admin/coordinador) ────────────────────
    @Transactional(readOnly = true)
    public List<CertificadoDto> listarTodos() {
        return certificadoRepository.findAll()
            .stream()
            .map(this::toCertificadoDto)
            .toList();
    }

    // ── Verificación pública por código QR ───────────────────────────────────
    // Este método no requiere autenticación
    @Transactional(readOnly = true)
    public VerificacionDto verificar(String codigoVerificacion) {
        Certificado certificado = certificadoRepository
            .findByCodigoVerificacion(codigoVerificacion)
            .orElseThrow(() -> new BusinessException(
                "Certificado no encontrado", 404));

        Inscripcion inscripcion = certificado.getInscripcion();
        Certificado ultimoCertificado = certificadoRepository
            .findFirstByInscripcion_IdInscripcionOrderByVersionDescIdCertificadoDesc(
                inscripcion.getIdInscripcion())
            .orElse(certificado);

        if (!ultimoCertificado.getIdCertificado().equals(certificado.getIdCertificado())
                || certificado.getEstadoEmision() != Certificado.EstadoEmision.GENERADO) {
            throw new BusinessException(
                "Este certificado no esta vigente. Verifica la ultima version emitida.", 400);
        }
        VerificacionDto dto = new VerificacionDto();

        // Estado legible para el usuario
        dto.setEstado("VALIDO");

        dto.setNombreTitular(
            inscripcion.getParticipante().getNombres()
            + " " + inscripcion.getParticipante().getApellidos());

        dto.setNombreActividad(inscripcion.getCurso() != null
            ? inscripcion.getCurso().getNombre()
            : inscripcion.getEvento().getNombre());

        dto.setCargaHoraria(inscripcion.getCurso() != null
            ? inscripcion.getCurso().getCargaHoraria()
            : inscripcion.getEvento().getCargaHoraria());

        // Nota solo para cursos
        if (inscripcion.getCurso() != null) {
            evaluacionRepository
                .findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion())
                .ifPresent(e -> dto.setNotaFinal(e.getNotaFinal().toString()));
        }

        dto.setFechaEmision(certificado.getFechaEmision());
        dto.setVersion(certificado.getVersion());

        // Si fue reemitido, incluir URL del reemplazo
        if (certificado.getEstadoEmision() == Certificado.EstadoEmision.REEMITIDO) {
            anulacionRepository.findByCertificado_IdCertificado(
                    certificado.getIdCertificado())
                .ifPresent(anulacion -> {
                    if (anulacion.getCertificadoReemplazo() != null) {
                        dto.setUrlCertificadoReemplazo(
                            baseUrl + "/verificar/"
                            + anulacion.getCertificadoReemplazo()
                                .getCodigoVerificacion());
                    }
                });
        }

        return dto;
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void validarRequisitos(Inscripcion inscripcion) {
        if (inscripcion.getCurso() != null) {
            // Curso: debe estar APROBADO
            EvaluacionEstudiante eval = evaluacionRepository
                .findByInscripcion_IdInscripcion(inscripcion.getIdInscripcion())
                .orElseThrow(() -> new BusinessException(
                    "El participante no tiene nota registrada", 400));

            if (eval.getEstado() != EvaluacionEstudiante.EstadoEvaluacion.APROBADO) {
                throw new BusinessException(
                    "El participante no aprobó el curso", 400);
            }
        } else {
            // Evento: debe tener registro de asistencia
            boolean tieneAsistencia = asistenciaRepository
                .existsByInscripcion_IdInscripcion(inscripcion.getIdInscripcion());

            if (!tieneAsistencia) {
                throw new BusinessException(
                    "La asistencia no fue registrada para este participante", 400);
            }
        }
    }

    // ── Método generarReemplazo — reemplazar completo ────────────────────────

    private Certificado generarReemplazo(Certificado original) {
        String nuevoCodigo = UUID.randomUUID().toString();
        int siguienteVersion = certificadoRepository
            .findFirstByInscripcion_IdInscripcionOrderByVersionDescIdCertificadoDesc(
                original.getInscripcion().getIdInscripcion())
            .map(c -> c.getVersion() + 1)
            .orElse(original.getVersion() + 1);

        Certificado reemplazo = Certificado.builder()
            .inscripcion(original.getInscripcion())
            .codigoVerificacion(nuevoCodigo)
            .estadoEmision(Certificado.EstadoEmision.GENERADO)
            .version(siguienteVersion)
            .build();

        reemplazo = certificadoRepository.save(reemplazo);

        // Obtener plantilla vigente para el reemplazo
        Long idCurso  = original.getInscripcion().getCurso()  != null
            ? original.getInscripcion().getCurso().getIdCurso()   : null;
        Long idEvento = original.getInscripcion().getEvento() != null
            ? original.getInscripcion().getEvento().getIdEvento() : null;

        String rutaPlantilla = plantillaService
            .obtenerVigente(idCurso, idEvento)
            .getArchivoPdf();

        String rutaPdf = pdfService.generarYGuardar(reemplazo, rutaPlantilla);
        reemplazo.setArchivoGenerado(rutaPdf);
        return certificadoRepository.save(reemplazo);
    }
    private Certificado buscarCertificado(Long idCertificado) {
        return certificadoRepository.findById(idCertificado)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Certificado", idCertificado));
    }

    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private CertificadoDto toCertificadoDto(Certificado c) {
        CertificadoDto dto = new CertificadoDto();
        dto.setIdCertificado(c.getIdCertificado());
        dto.setIdInscripcion(c.getInscripcion().getIdInscripcion());
        dto.setNombreParticipante(
            c.getInscripcion().getParticipante().getNombres()
            + " " + c.getInscripcion().getParticipante().getApellidos());
        dto.setEstadoEmision(c.getEstadoEmision().name());
        dto.setVersion(c.getVersion());
        dto.setCodigoVerificacion(c.getCodigoVerificacion());
        dto.setUrlVerificacion(baseUrl + "/verificar/" + c.getCodigoVerificacion());
        dto.setFechaEmision(c.getFechaEmision());

        Inscripcion i = c.getInscripcion();
        if (i.getCurso() != null) {
            dto.setNombreActividad(i.getCurso().getNombre());
            dto.setTipoActividad("CURSO");
            dto.setCargaHoraria(i.getCurso().getCargaHoraria());
            evaluacionRepository
                .findByInscripcion_IdInscripcion(i.getIdInscripcion())
                .ifPresent(e -> dto.setNotaFinal(e.getNotaFinal().toString()));
        } else {
            dto.setNombreActividad(i.getEvento().getNombre());
            dto.setTipoActividad("EVENTO");
            dto.setCargaHoraria(i.getEvento().getCargaHoraria());
        }

        return dto;
    }
}
