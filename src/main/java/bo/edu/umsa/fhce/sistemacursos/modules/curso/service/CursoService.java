// src/main/java/.../modules/curso/service/CursoService.java

package bo.edu.umsa.fhce.sistemacursos.modules.curso.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.Carrera;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.CarreraRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.CursoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.CursoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.AsignarDisenadorRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.ParaleloDto;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.ParaleloRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.ParaleloId;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.ParaleloRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.DocenteRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CursoService {

    private final CursoRepository    cursoRepository;
    private final ParaleloRepository paraleloRepository;
    private final CarreraRepository  carreraRepository;
    private final UsuarioRepository  usuarioRepository;
    private final DocenteRepository  docenteRepository;
    private final InscripcionRepository inscripcionRepository;

    // ── Listar cursos abiertos (catálogo público autenticado) ────────────────
    @Transactional(readOnly = true)
    public List<CursoDto> listarAbiertos(Long idCarrera) {
        return mapearCursos(cursoRepository.findAbiertos(idCarrera));
    }

    // ── Listar todos (admin y coordinador) ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<CursoDto> listarTodos(Long idCarrera) {
        List<Curso> cursos = (idCarrera != null)
            ? cursoRepository.findByCarrera_IdCarrera(idCarrera)
            : cursoRepository.findAll();
        return mapearCursos(cursos);
    }

    // ── Listar cursos asignados al disenador ───────────────────────────────
    @Transactional(readOnly = true)
    public List<CursoDto> listarAsignadosDisenador() {
        Usuario actual = getUsuarioActual();
        List<Curso> cursos = cursoRepository.findByDisenador_IdUsuario(actual.getIdUsuario());
        return mapearCursos(cursos);
    }

    // ── Obtener curso por id ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public CursoDto obtener(Long idCurso) {
        return mapearCursos(List.of(buscarCurso(idCurso))).get(0);
    }

    // ── Crear curso ──────────────────────────────────────────────────────────
    @Transactional
    public CursoDto crear(CursoRequest request) {
        Carrera carrera = carreraRepository.findById(request.getIdCarrera())
            .orElseThrow(() -> new ResourceNotFoundException("Carrera", request.getIdCarrera()));

        // El organizador es el usuario autenticado actual
        Usuario organizador = getUsuarioActual();

        // Verificar que el coordinador gestione esa carrera
        // (el admin puede crear en cualquier carrera)
        verificarAccesoCarrera(organizador, carrera);

        Curso curso = Curso.builder()
            .carrera(carrera)
            .organizador(organizador)
            .nombre(request.getNombre())
            .descripcion(request.getDescripcion())
            .imagen(request.getImagen())
            .cargaHoraria(request.getCargaHoraria())
            .duracion(request.getDuracion())
            .unidad(normalizeUnidadDuracion(request.getUnidad()))
            .fechaInicio(request.getFechaInicio())
            .costoExterno(request.getCostoExterno())
            .costoUmsa(request.getCostoUmsa())
            .notaAprobacion(request.getNotaAprobacion())
            .estado(Curso.EstadoCurso.ABIERTO)
            .build();

        curso = cursoRepository.save(curso);
        log.info("Curso creado: {} por {}", curso.getNombre(), organizador.getUsername());
        return toCursoDto(curso);
    }

    // ── Actualizar curso ─────────────────────────────────────────────────────
    @Transactional
    public CursoDto actualizar(Long idCurso, CursoRequest request) {
        Curso curso = buscarCurso(idCurso);
        verificarAccesoCarrera(getUsuarioActual(), curso.getCarrera());

        Carrera carrera = carreraRepository.findById(request.getIdCarrera())
            .orElseThrow(() -> new ResourceNotFoundException("Carrera", request.getIdCarrera()));

        curso.setCarrera(carrera);
        curso.setNombre(request.getNombre());
        curso.setDescripcion(request.getDescripcion());
        curso.setImagen(request.getImagen());
        curso.setCargaHoraria(request.getCargaHoraria());
        curso.setDuracion(request.getDuracion());
        curso.setUnidad(normalizeUnidadDuracion(request.getUnidad()));
        curso.setFechaInicio(request.getFechaInicio());
        curso.setCostoExterno(request.getCostoExterno());
        curso.setCostoUmsa(request.getCostoUmsa());
        curso.setNotaAprobacion(request.getNotaAprobacion());

        cursoRepository.save(curso);
        return toCursoDto(curso);
    }

    // ── Cambiar estado manualmente (ABIERTO / FINALIZADO) ───────────────────
    @Transactional
    public CursoDto cambiarEstado(Long idCurso, String estado) {
        Curso curso = buscarCurso(idCurso);
        try {
            curso.setEstado(Curso.EstadoCurso.valueOf(estado));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                "Estado inválido. Use: ABIERTO, LLENO o FINALIZADO", 400);
        }
        cursoRepository.save(curso);
        return toCursoDto(curso);
    }

    // ── Asignar disenador a curso ─────────────────────────────────────────
    @Transactional
    public CursoDto asignarDisenador(Long idCurso, AsignarDisenadorRequest request) {
        Curso curso = buscarCurso(idCurso);
        verificarAccesoCarrera(getUsuarioActual(), curso.getCarrera());

        if (request.getIdDisenador() == null) {
            curso.setDisenador(null);
            cursoRepository.save(curso);
            return toCursoDto(curso);
        }

        Usuario disenador = usuarioRepository.findById(request.getIdDisenador())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", request.getIdDisenador()));

        boolean esDisenador = disenador.getRoles().stream()
            .anyMatch(r -> normalizeRolName(r.getNombre()).equals("DISENADOR"));
        if (!esDisenador) {
            throw new BusinessException(
                "El usuario no tiene el rol DISENADOR", 400);
        }

        curso.setDisenador(disenador);
        cursoRepository.save(curso);
        return toCursoDto(curso);
    }

    // ── Eliminar curso ─────────────────────────────────────────────────────
    @Transactional
    public void eliminar(Long idCurso) {
        Curso curso = buscarCurso(idCurso);
        verificarAccesoCarrera(getUsuarioActual(), curso.getCarrera());
        cursoRepository.delete(curso);
        log.info("Curso eliminado: {}", idCurso);
    }

    // ── Agregar paralelo a un curso ──────────────────────────────────────────
    @Transactional
    public ParaleloDto agregarParalelo(Long idCurso, ParaleloRequest request) {
        Curso curso = buscarCurso(idCurso);

        // Verificar que el código no esté repetido en ese curso
        ParaleloId pk = new ParaleloId(idCurso, request.getCodigo());
        if (paraleloRepository.existsById(pk)) {
            throw new BusinessException(
                "Ya existe un paralelo con código '" + request.getCodigo()
                + "' en este curso", 409);
        }

        Paralelo paralelo = new Paralelo(curso, request.getCodigo());
        paralelo.setModalidad(Paralelo.Modalidad.valueOf(request.getModalidad()));
        paralelo.setCupoMaximo(request.getCupoMaximo());
        paralelo.setHorarioDescripcion(request.getHorarioDescripcion());
        paralelo.setLugar(request.getLugar());
        paralelo.setLink(request.getLink());

        // Asignar docente si se especificó
        if (request.getIdDocente() != null) {
            Usuario docente = usuarioRepository.findById(request.getIdDocente())
                .orElseThrow(() -> new ResourceNotFoundException("Docente", request.getIdDocente()));

            // Verificar que tenga rol DOCENTE
            boolean esDocente = docente.getRoles().stream()
                .anyMatch(r -> r.getNombre().equals("DOCENTE"));
            if (!esDocente) {
                throw new BusinessException(
                    "El usuario no tiene el rol DOCENTE", 400);
            }
            paralelo.setDocente(docente);
        }

        paralelo = paraleloRepository.save(paralelo);
        log.info("Paralelo {} agregado al curso {}", request.getCodigo(), idCurso);
        return toParaleloDto(paralelo);
    }

    // ── Actualizar paralelo ──────────────────────────────────────────────────
    @Transactional
    public ParaleloDto actualizarParalelo(Long idCurso, String codigo, ParaleloRequest request) {
        ParaleloId pk = new ParaleloId(idCurso, codigo);
        Paralelo paralelo = paraleloRepository.findById(pk)
            .orElseThrow(() -> new BusinessException(
                "Paralelo '" + codigo + "' no encontrado en el curso " + idCurso, 404));

        paralelo.setModalidad(Paralelo.Modalidad.valueOf(request.getModalidad()));
        paralelo.setCupoMaximo(request.getCupoMaximo());
        paralelo.setHorarioDescripcion(request.getHorarioDescripcion());
        paralelo.setLugar(request.getLugar());
        paralelo.setLink(request.getLink());

        if (request.getIdDocente() != null) {
            Usuario docente = usuarioRepository.findById(request.getIdDocente())
                .orElseThrow(() -> new ResourceNotFoundException("Docente", request.getIdDocente()));
            paralelo.setDocente(docente);
        } else {
            paralelo.setDocente(null); // permite desasignar docente
        }

        paraleloRepository.save(paralelo);
        return toParaleloDto(paralelo);
    }

    // ── Eliminar paralelo ────────────────────────────────────────────────────
    @Transactional
    public void eliminarParalelo(Long idCurso, String codigo) {
        ParaleloId pk = new ParaleloId(idCurso, codigo);
        if (!paraleloRepository.existsById(pk)) {
            throw new BusinessException(
                "Paralelo '" + codigo + "' no encontrado", 404);
        }
        paraleloRepository.deleteById(pk);
        log.info("Paralelo {} eliminado del curso {}", codigo, idCurso);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private Curso buscarCurso(Long idCurso) {
        return cursoRepository.findById(idCurso)
            .orElseThrow(() -> new ResourceNotFoundException("Curso", idCurso));
    }

    // Obtiene el usuario autenticado del SecurityContext
    private Usuario getUsuarioActual() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return usuarioRepository.findById(userDetails.getIdUsuario())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Usuario", userDetails.getIdUsuario()));
    }

    // Verifica que el usuario pueda gestionar la carrera
    // Admin puede todo — coordinador solo su carrera asignada
    private void verificarAccesoCarrera(Usuario usuario, Carrera carrera) {
        boolean esAdmin = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("ADMINISTRADOR"));
        if (esAdmin) return; // admin tiene acceso a todo

        // Verificar que el coordinador esté asignado a esa carrera
        boolean tieneAcceso = usuario.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("COORDINADOR"));
        if (!tieneAcceso) {
            throw new BusinessException(
                "No tienes permisos para gestionar el curso de la carrera " + carrera.getNombre(), 403);
        }
        // Nota: la verificación de carrera específica del coordinador
        // se implementa aquí cuando tengamos CoordinadorCarreraRepository inyectado
    }

    private List<CursoDto> mapearCursos(List<Curso> cursos) {
        if (cursos == null || cursos.isEmpty()) {
            return List.of();
        }

        List<Long> cursoIds = cursos.stream()
            .map(Curso::getIdCurso)
            .toList();

        Map<ParaleloId, Integer> inscritosPorParalelo = new HashMap<>();
        for (Object[] row : inscripcionRepository.contarConfirmadasPorCursoYParalelo(cursoIds)) {
            Long idCurso = (Long) row[0];
            String codigo = (String) row[1];
            Number total = (Number) row[2];
            inscritosPorParalelo.put(new ParaleloId(idCurso, codigo), total.intValue());
        }

        List<Long> docenteIds = cursos.stream()
            .flatMap(curso -> curso.getParalelos().stream())
            .map(Paralelo::getDocente)
            .filter(java.util.Objects::nonNull)
            .map(docente -> docente.getIdUsuario())
            .distinct()
            .toList();

        Map<Long, String> titulosDocentes = docenteIds.isEmpty()
            ? Map.of()
            : docenteRepository.findByIdUsuarioIn(docenteIds).stream()
                .collect(Collectors.toMap(
                    docente -> docente.getIdUsuario(),
                    docente -> docente.getTitulo()));

        return cursos.stream()
            .map(curso -> toCursoDto(curso, inscritosPorParalelo, titulosDocentes))
            .toList();
    }

    // Convierte Curso → CursoDto incluyendo sus paralelos
    private CursoDto toCursoDto(Curso c) {
        return toCursoDto(c, Map.of(), Map.of());
    }

    private CursoDto toCursoDto(Curso c, Map<ParaleloId, Integer> inscritosPorParalelo,
            Map<Long, String> titulosDocentes) {
        CursoDto dto = new CursoDto();
        dto.setIdCurso(c.getIdCurso());
        dto.setIdCarrera(c.getCarrera().getIdCarrera());
        dto.setNombreCarrera(c.getCarrera().getNombre());
        dto.setNombreOrganizador(
            c.getOrganizador().getNombres() + " " + c.getOrganizador().getApellidos());
        if (c.getDisenador() != null) {
            dto.setIdDisenador(c.getDisenador().getIdUsuario());
            dto.setNombreDisenador(
                c.getDisenador().getNombres() + " " + c.getDisenador().getApellidos());
        }
        dto.setNombre(c.getNombre());
        dto.setDescripcion(c.getDescripcion());
        dto.setImagen(c.getImagen());
        dto.setCargaHoraria(c.getCargaHoraria());
        dto.setDuracion(c.getDuracion());
        dto.setUnidad(c.getUnidad());
        dto.setFechaInicio(c.getFechaInicio());
        dto.setCostoExterno(c.getCostoExterno());
        dto.setCostoUmsa(c.getCostoUmsa());
        dto.setNotaAprobacion(c.getNotaAprobacion());
        dto.setEstado(c.getEstado().name());
        dto.setFechaCreacion(c.getFechaCreacion());
        dto.setParalelos(c.getParalelos().stream()
            .map(paralelo -> toParaleloDto(paralelo, inscritosPorParalelo, titulosDocentes))
            .toList());
        return dto;
    }

    private String normalizeRolName(String nombreRol) {
        if (nombreRol == null) return "";
        return nombreRol.replace("Ñ", "N").replace("ñ", "n").toUpperCase();
    }

    private String normalizeUnidadDuracion(String unidad) {
        if (unidad == null || unidad.isBlank()) {
            return null;
        }

        String normalized = unidad.trim().toLowerCase();
        if (normalized.equals("dias") || normalized.equals("días")) {
            return "días";
        }
        if (normalized.equals("semanas")) {
            return "semanas";
        }
        if (normalized.equals("meses")) {
            return "meses";
        }

        throw new BusinessException(
            "Unidad inválida. Use: días, semanas o meses", 400);
    }

    private ParaleloDto toParaleloDto(Paralelo p) {
        return toParaleloDto(p, Map.of(), Map.of());
    }

    private ParaleloDto toParaleloDto(Paralelo p, Map<ParaleloId, Integer> inscritosPorParalelo,
            Map<Long, String> titulosDocentes) {
        ParaleloDto dto = new ParaleloDto();
        dto.setCodigo(p.getId().getCodigo());
        dto.setIdCurso(p.getId().getIdCurso());
        dto.setModalidad(p.getModalidad().name());
        dto.setCupoMaximo(p.getCupoMaximo());
        dto.setHorarioDescripcion(p.getHorarioDescripcion());
        dto.setLugar(p.getLugar());
        dto.setLink(p.getLink());

        if (p.getDocente() != null) {
            dto.setIdDocente(p.getDocente().getIdUsuario());
            dto.setNombreDocente(
                p.getDocente().getNombres() + " " + p.getDocente().getApellidos());
            dto.setTituloDocente(titulosDocentes.get(p.getDocente().getIdUsuario()));
        }

        int inscritos = inscritosPorParalelo.getOrDefault(p.getId(), 0);
        dto.setInscritos(inscritos);

        if (p.getCupoMaximo() != null) {
            dto.setCuposDisponibles(Math.max(0, p.getCupoMaximo() - inscritos));
        }

        return dto;
    }
}