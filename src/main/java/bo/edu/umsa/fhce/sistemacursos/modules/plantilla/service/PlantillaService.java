package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.io.InputStream;
import java.text.Normalizer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.AprobacionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.AprobacionRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.PlantillaEstadoResumenDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.PlantillaDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity.Aprobacion;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.entity.PlantillaCertificado;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.repository.AprobacionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.repository.PlantillaRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.CoordinadorCarreraRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlantillaService {

    private final PlantillaRepository  plantillaRepository;
    private final AprobacionRepository aprobacionRepository;
    private final CursoRepository      cursoRepository;
    private final EventoRepository     eventoRepository;
    private final CoordinadorCarreraRepository coordinadorCarreraRepository;
    private final UsuarioRepository    usuarioRepository;

    @Value("${app.plantillas.directorio:plantillas}")
    private String directorioPlantillas;

    @Value("${app.plantillas.max-size-mb:5}")
    private long maxPlantillaSizeMb;

    // ── Subir plantilla (diseñador) ──────────────────────────────────────────
    @Transactional
    public PlantillaDto subirPlantilla(MultipartFile archivo,
                                       Long idCurso,
                                       Long idEvento) throws IOException {
        validarActividad(idCurso, idEvento);
        validarArchivoPdf(archivo);

        Usuario disenador = getUsuarioActual();

        Actividad actividad = obtenerActividad(idCurso, idEvento);

        archivarPlantillasPendientes(actividad);

        // Calcular versión — es la siguiente a la última existente
        int nuevaVersion = calcularSiguienteVersion(idCurso, idEvento);

        // Guardar el archivo en disco
        String rutaArchivo = guardarArchivo(archivo, disenador.getIdUsuario(),
            actividad.idCurso(), actividad.idEvento(), nuevaVersion);

        PlantillaCertificado plantilla = PlantillaCertificado.builder()
            .curso(actividad.curso())
            .evento(actividad.evento())
            .archivoPdf(rutaArchivo)
            .version(nuevaVersion)
            .subidaPor(disenador)
            .estado(PlantillaCertificado.EstadoPlantilla.PENDIENTE)
            .build();

        plantilla = plantillaRepository.save(plantilla);

        log.info("Plantilla v{} subida por {} para {} {}",
            nuevaVersion, disenador.getUsername(),
            actividad.idCurso() != null ? "curso" : "evento",
            actividad.idCurso() != null ? actividad.idCurso() : actividad.idEvento());

        return toPlantillaDto(plantilla);
    }

    // ── Revisar plantilla (coordinador) ─────────────────────────────────────
    @Transactional
    public PlantillaDto revisar(Long idPlantilla, AprobacionRequest request) {
        PlantillaCertificado plantilla = buscarPlantilla(idPlantilla);
        Usuario coordinador = getUsuarioActual();

        validarCoordinadorDeActividad(coordinador, plantilla);
        validarPlantillaPendiente(plantilla);

        Aprobacion.EstadoAprobacion nuevoEstado = parseEstadoRevision(request);
        validarObservaciones(request, nuevoEstado);

        // Registrar la revisión
        Aprobacion aprobacion = Aprobacion.builder()
            .plantilla(plantilla)
            .coordinador(coordinador)
            .estado(nuevoEstado)
            .observaciones(request.getObservaciones())
            .build();
        aprobacionRepository.save(aprobacion);

        actualizarEstadoPlantilla(plantilla, idPlantilla, request, nuevoEstado);

        plantillaRepository.save(plantilla);
        return toPlantillaDto(plantilla);
    }

    // ── Descargar plantilla para revisión ────────────────────────────────────
    @Transactional(readOnly = true)
    public byte[] descargarPlantilla(Long idPlantilla) throws IOException {
        PlantillaCertificado plantilla = buscarPlantilla(idPlantilla);
        validarPermisoDescarga(plantilla);
        Path ruta = Paths.get(plantilla.getArchivoPdf());

        if (!Files.exists(ruta)) {
            throw new BusinessException(
                "Archivo de plantilla no encontrado en el servidor", 500);
        }

        return Files.readAllBytes(ruta);
    }

    // ── Obtener plantilla VIGENTE de una actividad ───────────────────────────
    // Usada por CertificadoService antes de generar el PDF
    @Transactional(readOnly = true)
    public PlantillaCertificado obtenerVigente(Long idCurso, Long idEvento) {
        if (idCurso != null) {
            return plantillaRepository.findByCurso_IdCursoAndEstado(
                idCurso, PlantillaCertificado.EstadoPlantilla.VIGENTE)
                .orElseThrow(() -> new BusinessException(
                    "No hay una plantilla aprobada para este curso. "
                    + "El diseñador debe subir una plantilla y el coordinador aprobarla.", 400));
        } else {
            return plantillaRepository.findByEvento_IdEventoAndEstado(
                idEvento, PlantillaCertificado.EstadoPlantilla.VIGENTE)
                .orElseThrow(() -> new BusinessException(
                    "No hay una plantilla aprobada para este evento.", 400));
        }
    }

    // ── Listar plantillas pendientes (bandeja coordinador) ───────────────────
    @Transactional(readOnly = true)
    public List<PlantillaDto> listarPendientes() {
        Usuario actual = getUsuarioActual();
        Set<Long> carrerasPermitidas = coordinadorCarreraRepository
            .findByIdCoordinador(actual.getIdUsuario())
            .stream()
            .map(cc -> cc.getCarrera().getIdCarrera())
            .collect(java.util.stream.Collectors.toSet());

        Map<String, PlantillaCertificado> ultimasPendientes = new LinkedHashMap<>();

        plantillaRepository
            .findByEstadoOrderByFechaSubidaAsc(
                PlantillaCertificado.EstadoPlantilla.PENDIENTE)
            .stream()
            .filter(plantilla -> perteneceACarrerasDelCoordinador(plantilla, carrerasPermitidas))
            .forEach(plantilla -> ultimasPendientes.put(
                actividadKey(plantilla),
                plantilla));

        return ultimasPendientes.values().stream()
            .map(this::toPlantillaDto)
            .toList();
    }

    // ── Listar mis plantillas (diseñador) ────────────────────────────────────
    @Transactional(readOnly = true)
    public List<PlantillaDto> misPlantillas() {
        Usuario actual = getUsuarioActual();
        return plantillaRepository
            .findBySubidaPor_IdUsuario(actual.getIdUsuario())
            .stream()
            .map(this::toPlantillaDto)
            .toList();
    }

    // ── Historial de versiones de plantillas de una actividad ────────────────
    @Transactional(readOnly = true)
    public List<PlantillaDto> historial(Long idCurso, Long idEvento) {
        List<PlantillaCertificado> plantillas = (idCurso != null)
            ? plantillaRepository.findByCurso_IdCursoOrderByVersionDesc(idCurso)
            : plantillaRepository.findByEvento_IdEventoOrderByVersionDesc(idEvento);

        return plantillas.stream().map(this::toPlantillaDto).toList();
    }

    // ── Resumen del estado más reciente de plantillas por actividad ─────────
    @Transactional(readOnly = true)
    public List<PlantillaEstadoResumenDto> estadosPorActividad() {
        Map<String, PlantillaCertificado> ultimasPorActividad = new LinkedHashMap<>();

        plantillaRepository.findAll().forEach(plantilla -> {
            String key = plantilla.getCurso() != null
                ? "CURSO-" + plantilla.getCurso().getIdCurso()
                : "EVENTO-" + plantilla.getEvento().getIdEvento();

            PlantillaCertificado actual = ultimasPorActividad.get(key);
            if (actual == null || plantilla.getVersion() > actual.getVersion()) {
                ultimasPorActividad.put(key, plantilla);
            }
        });

        return ultimasPorActividad.values().stream()
            .map(plantilla -> {
                PlantillaEstadoResumenDto dto = new PlantillaEstadoResumenDto();
                if (plantilla.getCurso() != null) {
                    dto.setIdCurso(plantilla.getCurso().getIdCurso());
                }
                if (plantilla.getEvento() != null) {
                    dto.setIdEvento(plantilla.getEvento().getIdEvento());
                }
                dto.setEstado(plantilla.getEstado().name());
                dto.setVersion(plantilla.getVersion());
                return dto;
            })
            .toList();
    }

    // ── Historial de revisiones de una plantilla ────────────────────────────
    @Transactional(readOnly = true)
    public List<AprobacionDto> historialAprobaciones(Long idPlantilla) {
        PlantillaCertificado plantilla = buscarPlantilla(idPlantilla);
        validarPermisoVerAprobaciones(plantilla);

        return aprobacionRepository
            .findByPlantilla_IdPlantillaOrderByFechaRevisionDesc(idPlantilla)
            .stream()
            .map(this::toAprobacionDto)
            .toList();
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private boolean esPdf(MultipartFile archivo) {
        String contentType = archivo.getContentType();
        boolean pdfPorTipo = "application/pdf".equals(contentType);
        boolean pdfPorNombre = archivo.getOriginalFilename() != null
            && archivo.getOriginalFilename().toLowerCase().endsWith(".pdf");

        return (pdfPorTipo || pdfPorNombre) && tieneFirmaPdf(archivo);
    }

    private void validarActividad(Long idCurso, Long idEvento) {
        if (idCurso == null && idEvento == null) {
            throw new BusinessException(
                "Debe especificar un curso o un evento", 400);
        }
        if (idCurso != null && idEvento != null) {
            throw new BusinessException(
                "Una plantilla pertenece a un curso O a un evento, no a ambos", 400);
        }
    }

    private void validarArchivoPdf(MultipartFile archivo) {
        if (archivo.isEmpty() || !esPdf(archivo)) {
            throw new BusinessException(
                "El archivo debe ser un PDF válido", 400);
        }

        validarSize(archivo);
    }

    private Actividad obtenerActividad(Long idCurso, Long idEvento) {
        if (idCurso != null) {
            Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> new ResourceNotFoundException("Curso", idCurso));
            return new Actividad(curso, null, idCurso, null);
        }

        Evento evento = eventoRepository.findById(idEvento)
            .orElseThrow(() -> new ResourceNotFoundException("Evento", idEvento));
        return new Actividad(null, evento, null, idEvento);
    }

    private boolean perteneceACarrerasDelCoordinador(PlantillaCertificado plantilla, Set<Long> carrerasPermitidas) {
        if (carrerasPermitidas.isEmpty()) {
            return false;
        }

        if (plantilla.getCurso() != null && plantilla.getCurso().getCarrera() != null) {
            return carrerasPermitidas.contains(plantilla.getCurso().getCarrera().getIdCarrera());
        }

        if (plantilla.getEvento() != null && plantilla.getEvento().getCarrera() != null) {
            return carrerasPermitidas.contains(plantilla.getEvento().getCarrera().getIdCarrera());
        }

        return false;
    }

    private void validarPlantillaPendiente(PlantillaCertificado plantilla) {
        if (plantilla.getEstado() != PlantillaCertificado.EstadoPlantilla.PENDIENTE) {
            throw new BusinessException(
                "Solo se pueden revisar plantillas en estado PENDIENTE. "
                + "Estado actual: " + plantilla.getEstado(), 400);
        }
    }

    private Aprobacion.EstadoAprobacion parseEstadoRevision(AprobacionRequest request) {
        try {
            return Aprobacion.EstadoAprobacion.valueOf(request.getEstado());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                "Estado inválido. Use: APROBADA o RECHAZADA", 400);
        }
    }

    private void validarObservaciones(AprobacionRequest request,
                                      Aprobacion.EstadoAprobacion nuevoEstado) {
        if (nuevoEstado == Aprobacion.EstadoAprobacion.RECHAZADA
                && (request.getObservaciones() == null
                    || request.getObservaciones().isBlank())) {
            throw new BusinessException(
                "Las observaciones son obligatorias al rechazar una plantilla", 400);
        }
    }

    private void actualizarEstadoPlantilla(PlantillaCertificado plantilla,
                                           Long idPlantilla,
                                           AprobacionRequest request,
                                           Aprobacion.EstadoAprobacion nuevoEstado) {
        if (nuevoEstado == Aprobacion.EstadoAprobacion.APROBADA) {
            archivarPlantillasVigentes(plantilla);
            plantilla.setEstado(PlantillaCertificado.EstadoPlantilla.VIGENTE);
            log.info("Plantilla {} aprobada — ahora es VIGENTE", idPlantilla);
            return;
        }

        plantilla.setEstado(PlantillaCertificado.EstadoPlantilla.HISTORICA);
        log.info("Plantilla {} rechazada — observaciones: {}",
            idPlantilla, request.getObservaciones());
    }

    private void archivarPlantillasVigentes(PlantillaCertificado plantilla) {
        if (plantilla.getCurso() != null) {
            plantillaRepository.archivarVigentesDeCurso(
                plantilla.getCurso().getIdCurso());
            return;
        }

        plantillaRepository.archivarVigentesDeEvento(
            plantilla.getEvento().getIdEvento());
    }

    private void archivarPlantillasPendientes(Actividad actividad) {
        if (actividad.idCurso() != null) {
            plantillaRepository.archivarPendientesDeCurso(actividad.idCurso());
            return;
        }

        plantillaRepository.archivarPendientesDeEvento(actividad.idEvento());
    }

    private String actividadKey(PlantillaCertificado plantilla) {
        if (plantilla.getCurso() != null) {
            return "CURSO-" + plantilla.getCurso().getIdCurso();
        }

        return "EVENTO-" + plantilla.getEvento().getIdEvento();
    }

    private boolean tieneFirmaPdf(MultipartFile archivo) {
        try (InputStream input = archivo.getInputStream()) {
            byte[] header = new byte[5];
            int read = input.read(header);
            if (read < 5) {
                return false;
            }
            return header[0] == '%' && header[1] == 'P'
                && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
        } catch (IOException e) {
            return false;
        }
    }

    private void validarSize(MultipartFile archivo) {
        long maxBytes = maxPlantillaSizeMb * 1024 * 1024;
        if (archivo.getSize() > maxBytes) {
            throw new BusinessException(
                "El archivo supera el tamaño máximo permitido", 413);
        }
    }

    private int calcularSiguienteVersion(Long idCurso, Long idEvento) {
        List<PlantillaCertificado> existentes = (idCurso != null)
            ? plantillaRepository.findByCurso_IdCursoOrderByVersionDesc(idCurso)
            : plantillaRepository.findByEvento_IdEventoOrderByVersionDesc(idEvento);

        return existentes.isEmpty() ? 1 : existentes.get(0).getVersion() + 1;
    }

    private String guardarArchivo(MultipartFile archivo, Long idUsuario,
                                   Long idCurso, Long idEvento,
                                   int version) throws IOException {
        Path dirPath = Paths.get(directorioPlantillas)
            .toAbsolutePath()
            .normalize();
        Files.createDirectories(dirPath);

        // Nombre único: plantilla_{tipo}_{id}_v{version}_{uuid}.pdf
        String tipo = idCurso != null ? "curso" : "evento";
        Long idActividad = idCurso != null ? idCurso : idEvento;
        String nombre = String.format("plantilla_%s_%d_v%d_%s.pdf",
            tipo, idActividad, version,
            UUID.randomUUID().toString().substring(0, 8));

        Path rutaArchivo = dirPath.resolve(nombre);
        try (InputStream input = archivo.getInputStream()) {
            Files.copy(input, rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        }
        return rutaArchivo.toString();
    }

    private PlantillaCertificado buscarPlantilla(Long idPlantilla) {
        return plantillaRepository.findById(idPlantilla)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Plantilla", idPlantilla));
    }

    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    private PlantillaDto toPlantillaDto(PlantillaCertificado p) {
        PlantillaDto dto = new PlantillaDto();
        dto.setIdPlantilla(p.getIdPlantilla());
        dto.setVersion(p.getVersion());
        dto.setEstado(p.getEstado().name());
        dto.setFechaSubida(p.getFechaSubida());
        dto.setSubidaPor(
            p.getSubidaPor().getNombres() + " " + p.getSubidaPor().getApellidos());

        if (p.getCurso() != null) {
            dto.setIdCurso(p.getCurso().getIdCurso());
            dto.setNombreActividad(p.getCurso().getNombre());
            dto.setTipoActividad("CURSO");
        } else {
            dto.setIdEvento(p.getEvento().getIdEvento());
            dto.setNombreActividad(p.getEvento().getNombre());
            dto.setTipoActividad("EVENTO");
        }

        // Última observación del coordinador
        aprobacionRepository
            .findTopByPlantilla_IdPlantillaOrderByFechaRevisionDesc(p.getIdPlantilla())
            .ifPresent(a -> dto.setUltimaObservacion(a.getObservaciones()));

        return dto;
    }

    private AprobacionDto toAprobacionDto(Aprobacion aprobacion) {
        AprobacionDto dto = new AprobacionDto();
        dto.setIdAprobacion(aprobacion.getIdAprobacion());
        dto.setEstado(aprobacion.getEstado().name());
        dto.setObservaciones(aprobacion.getObservaciones());
        dto.setFechaRevision(aprobacion.getFechaRevision());
        dto.setCoordinador(aprobacion.getCoordinador().getNombres()
            + " " + aprobacion.getCoordinador().getApellidos());
        return dto;
    }

    private record Actividad(Curso curso, Evento evento, Long idCurso, Long idEvento) {
    }

    private void validarPermisoDescarga(PlantillaCertificado plantilla) {
        Usuario actual = getUsuarioActual();

        boolean esAdmin = tieneRol(actual, "ADMINISTRADOR");
        if (esAdmin) {
            return;
        }

        boolean esCoordinador = tieneRol(actual, "COORDINADOR");
        if (esCoordinador) {
            return;
        }

        boolean esDisenador = tieneRol(actual, "DISENADOR", "DISEÑADOR");
        if (esDisenador
                && plantilla.getSubidaPor() != null
                && actual.getIdUsuario().equals(plantilla.getSubidaPor().getIdUsuario())) {
            return;
        }

        throw new BusinessException(
            "No tienes permisos para descargar esta plantilla", 403);
    }

    private void validarPermisoVerAprobaciones(PlantillaCertificado plantilla) {
        Usuario actual = getUsuarioActual();

        boolean esAdmin = tieneRol(actual, "ADMINISTRADOR");
        if (esAdmin) {
            return;
        }

        boolean esCoordinador = tieneRol(actual, "COORDINADOR");
        if (esCoordinador) {
            return;
        }

        boolean esDisenador = tieneRol(actual, "DISENADOR", "DISEÑADOR");
        if (esDisenador
                && plantilla.getSubidaPor() != null
                && actual.getIdUsuario().equals(plantilla.getSubidaPor().getIdUsuario())) {
            return;
        }

        throw new BusinessException(
            "No tienes permisos para ver las revisiones de esta plantilla", 403);
    }

    private void validarCoordinadorDeActividad(Usuario coordinador,
                                               PlantillaCertificado plantilla) {
        boolean esAdmin = tieneRol(coordinador, "ADMINISTRADOR");
        if (esAdmin) {
            return;
        }

        boolean esCoordinador = tieneRol(coordinador, "COORDINADOR");
        if (!esCoordinador) {
            throw new BusinessException(
                "No tienes permisos para revisar plantillas", 403);
        }

        Long idCarrera = null;
        if (plantilla.getCurso() != null) {
            idCarrera = plantilla.getCurso().getCarrera().getIdCarrera();
        } else if (plantilla.getEvento() != null) {
            idCarrera = plantilla.getEvento().getCarrera().getIdCarrera();
        }

        if (idCarrera == null) {
            throw new BusinessException("No se pudo determinar la carrera", 500);
        }

        boolean asignado = coordinadorCarreraRepository
            .existsByCoordinador_IdUsuarioAndCarrera_IdCarrera(
                coordinador.getIdUsuario(), idCarrera);

        if (!asignado) {
            throw new BusinessException(
                "No eres coordinador de la carrera de esta actividad", 403);
        }
    }

    private boolean tieneRol(Usuario usuario, String... roles) {
        return usuario.getRoles().stream()
            .map(rol -> normalizarRol(rol.getNombre()))
            .anyMatch(normalizado -> {
                for (String rol : roles) {
                    if (normalizado.equals(normalizarRol(rol))) {
                        return true;
                    }
                }
                return false;
            });
    }

    private String normalizarRol(String rol) {
        if (rol == null) {
            return "";
        }
        String trimmed = rol.trim();
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return normalized.toUpperCase();
    }
}