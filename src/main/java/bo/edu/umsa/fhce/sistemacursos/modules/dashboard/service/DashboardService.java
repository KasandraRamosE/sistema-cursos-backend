package bo.edu.umsa.fhce.sistemacursos.modules.dashboard.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto.AdminDashboardDto;
import bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto.DashboardActividadDto;
import bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto.DashboardStatsDto;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Paralelo;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.repository.CursoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.repository.EventoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Inscripcion;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.InscripcionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.repository.CertificadoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.SolicitudEmision;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.repository.SolicitudEmisionRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CertificadoRepository certificadoRepository;
    private final SolicitudEmisionRepository solicitudEmisionRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto obtenerDashboardAdmin() {
        AdminDashboardDto response = new AdminDashboardDto();
        response.setStats(buildStats());
        response.setActividadesRecientes(buildActividadesRecientes());
        return response;
    }

    private DashboardStatsDto buildStats() {
        DashboardStatsDto stats = new DashboardStatsDto();
        long totalUsuarios = usuarioRepository.count();
        long internos = usuarioRepository.countByPasswordHashIsNull();
        long externos = usuarioRepository.countByPasswordHashIsNotNull();

        long totalCursos = cursoRepository.count();
        long totalEventos = eventoRepository.count();

        stats.setTotalUsuarios(totalUsuarios);
        stats.setUsuariosInternos(internos);
        stats.setUsuariosExternos(externos);
        stats.setTotalActividades(totalCursos + totalEventos);
        stats.setCursosActivos(cursoRepository.countByEstado(Curso.EstadoCurso.ABIERTO));
        stats.setEventosActivos(eventoRepository.countByEstado(Evento.EstadoEvento.ABIERTO));
        stats.setTotalInscripciones(inscripcionRepository.count());
        stats.setInscripcionesActivas(inscripcionRepository.countByEstado(Inscripcion.EstadoInscripcion.CONFIRMADA));
        stats.setTotalCertificados(certificadoRepository.count());
        stats.setCertificadosPendientes(
            solicitudEmisionRepository.countByEstado(SolicitudEmision.EstadoSolicitud.PENDIENTE));

        return stats;
    }

    private List<DashboardActividadDto> buildActividadesRecientes() {
        List<ActividadReciente> recientes = new ArrayList<>();

        for (Curso curso : cursoRepository.findAll()) {
            recientes.add(new ActividadReciente(
                buildCursoDto(curso),
                curso.getFechaCreacion()
            ));
        }

        for (Evento evento : eventoRepository.findAll()) {
            recientes.add(new ActividadReciente(
                buildEventoDto(evento),
                evento.getFechaCreacion()
            ));
        }

        return recientes.stream()
            .sorted(Comparator.comparing(ActividadReciente::fecha).reversed())
            .limit(8)
            .map(ActividadReciente::dto)
            .toList();
    }

    private DashboardActividadDto buildCursoDto(Curso curso) {
        DashboardActividadDto dto = new DashboardActividadDto();
        dto.setId("CURSO-" + curso.getIdCurso());
        dto.setNombre(curso.getNombre());
        dto.setTipo("CURSO");
        dto.setCarrera(curso.getCarrera().getNombre());
        dto.setEstado(curso.getEstado().name());

        long inscritos = inscripcionRepository.countByCurso_IdCursoAndEstado(
            curso.getIdCurso(), Inscripcion.EstadoInscripcion.CONFIRMADA);
        dto.setInscritos((int) inscritos);

        Integer cupoTotal = null;
        for (Paralelo paralelo : curso.getParalelos()) {
            if (paralelo.getCupoMaximo() == null) {
                cupoTotal = null;
                break;
            }
            cupoTotal = (cupoTotal == null ? 0 : cupoTotal) + paralelo.getCupoMaximo();
        }
        dto.setCupo(cupoTotal);
        return dto;
    }

    private DashboardActividadDto buildEventoDto(Evento evento) {
        DashboardActividadDto dto = new DashboardActividadDto();
        dto.setId("EVENTO-" + evento.getIdEvento());
        dto.setNombre(evento.getNombre());
        dto.setTipo("EVENTO");
        dto.setCarrera(evento.getCarrera().getNombre());
        dto.setEstado(evento.getEstado().name());

        long inscritos = inscripcionRepository.countByEvento_IdEventoAndEstado(
            evento.getIdEvento(), Inscripcion.EstadoInscripcion.CONFIRMADA);
        dto.setInscritos((int) inscritos);
        dto.setCupo(evento.getCupoMaximo());
        return dto;
    }

    private record ActividadReciente(DashboardActividadDto dto, LocalDateTime fecha) {}
}
