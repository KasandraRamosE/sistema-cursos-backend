// src/main/java/.../modules/carrera/service/CarreraService.java

package bo.edu.umsa.fhce.sistemacursos.modules.carrera.service;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.exception.ResourceNotFoundException;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.*;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.repository.*;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarreraService {

    private final CarreraRepository             carreraRepository;
    private final CoordinadorCarreraRepository  coordinadorCarreraRepository;
    private final UsuarioRepository             usuarioRepository;
    private final ModelMapper                   modelMapper;

    // ── Listar carreras activas ──────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CarreraDto> listarActivas() {
        return carreraRepository.findByEstado(Carrera.EstadoCarrera.ACTIVA)
            .stream()
            .map(c -> modelMapper.map(c, CarreraDto.class))
            .toList();
    }

    // ── Listar todas (admin) ─────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<CarreraDto> listarTodas() {
        return carreraRepository.findAll()
            .stream()
            .map(c -> modelMapper.map(c, CarreraDto.class))
            .toList();
    }

    // ── Crear carrera ────────────────────────────────────────────────────────
    @Transactional
    public CarreraDto crear(CarreraRequest request) {
        Carrera carrera = Carrera.builder()
            .nombre(request.getNombre())
            .estado(Carrera.EstadoCarrera.ACTIVA)
            .build();

        carrera = carreraRepository.save(carrera);
        log.info("Carrera creada: {}", carrera.getNombre());
        return modelMapper.map(carrera, CarreraDto.class);
    }

    // ── Actualizar nombre ────────────────────────────────────────────────────
    @Transactional
    public CarreraDto actualizar(Long idCarrera, CarreraRequest request) {
        Carrera carrera = buscarCarrera(idCarrera);
        carrera.setNombre(request.getNombre());
        carreraRepository.save(carrera);
        return modelMapper.map(carrera, CarreraDto.class);
    }

    // ── Activar / desactivar ─────────────────────────────────────────────────
    @Transactional
    public CarreraDto cambiarEstado(Long idCarrera, String estado) {
        Carrera carrera = buscarCarrera(idCarrera);
        try {
            carrera.setEstado(Carrera.EstadoCarrera.valueOf(estado));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                "Estado inválido: " + estado + ". Use ACTIVA o INACTIVA", 400);
        }
        carreraRepository.save(carrera);
        return modelMapper.map(carrera, CarreraDto.class);
    }

    // ── Asignar coordinador a carrera ────────────────────────────────────────
    @Transactional
    public void asignarCoordinador(Long idCarrera, AsignarCoordinadorRequest request) {
        Carrera carrera = buscarCarrera(idCarrera);

        Usuario coordinador = usuarioRepository.findById(request.getIdCoordinador())
            .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.getIdCoordinador()));

        // Verificar que el usuario tenga rol COORDINADOR
        boolean esCoordinador = coordinador.getRoles().stream()
            .anyMatch(r -> r.getNombre().equals("COORDINADOR"));
        if (!esCoordinador) {
            throw new BusinessException(
                "El usuario no tiene el rol COORDINADOR", 400);
        }

        // Verificar que no esté ya asignado
        CoordinadorCarreraId pk = new CoordinadorCarreraId(
            coordinador.getIdUsuario(), idCarrera);
        if (coordinadorCarreraRepository.existsById(pk)) {
            throw new BusinessException(
                "El coordinador ya está asignado a esta carrera", 409);
        }

        CoordinadorCarrera asignacion = new CoordinadorCarrera(coordinador, carrera);
        coordinadorCarreraRepository.save(asignacion);

        log.info("Coordinador {} asignado a carrera {}",
            coordinador.getUsername(), carrera.getNombre());
    }

    // ── Remover coordinador de carrera ───────────────────────────────────────
    @Transactional
    public void removerCoordinador(Long idCarrera, Long idCoordinador) {
        CoordinadorCarreraId pk = new CoordinadorCarreraId(idCoordinador, idCarrera);

        if (!coordinadorCarreraRepository.existsById(pk)) {
            throw new BusinessException(
                "El coordinador no está asignado a esta carrera", 404);
        }

        coordinadorCarreraRepository.deleteById(pk);
        log.info("Coordinador {} removido de carrera {}", idCoordinador, idCarrera);
    }

    // ── Listar coordinadores de una carrera ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<String> listarCoordinadores(Long idCarrera) {
        buscarCarrera(idCarrera); // valida que exista
        return coordinadorCarreraRepository.findByIdCarrera(idCarrera)
            .stream()
            .map(cc -> cc.getCoordinador().getNombres()
                + " " + cc.getCoordinador().getApellidos())
            .toList();
    }

    // ── Listar carreras de un coordinador ───────────────────────────────────
    @Transactional(readOnly = true)
    public List<CarreraDto> listarCarrerasDeCoordinador(Long idCoordinador) {
        return coordinadorCarreraRepository.findByIdCoordinador(idCoordinador)
            .stream()
            .map(cc -> modelMapper.map(cc.getCarrera(), CarreraDto.class))
            .toList();
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Carrera buscarCarrera(Long idCarrera) {
        return carreraRepository.findById(idCarrera)
            .orElseThrow(() -> new ResourceNotFoundException("Carrera", idCarrera));
    }
}