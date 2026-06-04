package bo.edu.umsa.fhce.sistemacursos.modules.usuario.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.AsignarRolRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.CambiarEstadoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.CambiarPasswordRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.ActualizarCarrerasRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.ActualizarPerfilRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.ActualizarEventosRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.ActualizarParalelosRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.ParaleloRefRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.RolDto;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.UsuarioDetalleDto;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.dto.UsuarioResumenDto;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // GET /api/usuarios
    // Solo ADMINISTRADOR puede listar todos los usuarios
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<List<UsuarioResumenDto>> listar(
            // Filtro opcional por rol: /api/usuarios?rol=DOCENTE
            @RequestParam(required = false) String rol) {

        List<UsuarioResumenDto> usuarios = (rol != null)
            ? usuarioService.listarPorRol(rol)
            : usuarioService.listarUsuarios();

        return ResponseEntity.ok(usuarios);
    }

    // GET /api/usuarios/docentes
    // ADMIN y COORDINADOR pueden listar docentes
    @GetMapping("/docentes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar usuarios con rol DOCENTE")
    public ResponseEntity<List<UsuarioResumenDto>> listarDocentes() {
        return ResponseEntity.ok(usuarioService.listarPorRol("DOCENTE"));
    }

    // GET /api/usuarios/participantes
    // ADMIN y COORDINADOR pueden listar participantes
    @GetMapping("/participantes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar usuarios con rol PARTICIPANTE")
    public ResponseEntity<List<UsuarioResumenDto>> listarParticipantes() {
        return ResponseEntity.ok(usuarioService.listarPorRol("PARTICIPANTE"));
    }

    // GET /api/usuarios/auxiliares
    // ADMIN y COORDINADOR pueden listar auxiliares
    @GetMapping("/auxiliares")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar usuarios con rol AUXILIAR")
    public ResponseEntity<List<UsuarioResumenDto>> listarAuxiliares() {
        return ResponseEntity.ok(usuarioService.listarPorRol("AUXILIAR"));
    }

    // GET /api/usuarios/disenadores
    // ADMIN y COORDINADOR pueden listar disenadores
    @GetMapping("/disenadores")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar usuarios con rol DISENADOR")
    public ResponseEntity<List<UsuarioResumenDto>> listarDisenadores() {
        return ResponseEntity.ok(usuarioService.listarPorRol("DISENADOR"));
    }

    // GET /api/usuarios/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Ver detalle de un usuario")
    public ResponseEntity<UsuarioDetalleDto> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerDetalle(id));
    }

    // GET /api/usuarios/me
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener perfil del usuario autenticado")
    public ResponseEntity<UsuarioDetalleDto> perfilActual() {
        return ResponseEntity.ok(usuarioService.obtenerActual());
    }

    // PUT /api/usuarios/me
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Actualizar perfil del usuario autenticado")
    public ResponseEntity<UsuarioDetalleDto> actualizarPerfil(
            @Valid @RequestBody ActualizarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarPerfilExterno(request));
    }

    // POST /api/usuarios/me/password
    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cambiar contrasena del usuario autenticado")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequest request) {
        usuarioService.cambiarPasswordExterno(request);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/usuarios/{id}/estado
    // PATCH es correcto aquí porque solo modificamos un campo, no el recurso completo
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Activar o desactivar un usuario")
    public ResponseEntity<UsuarioResumenDto> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request) {
        return ResponseEntity.ok(usuarioService.cambiarEstado(id, request));
    }

    // POST /api/usuarios/{id}/roles
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Asignar rol a usuario",
               description = "Para DOCENTE requiere 'titulo'. Para PARTICIPANTE requiere 'tipoParticipante'.")
    public ResponseEntity<UsuarioDetalleDto> asignarRol(
            @PathVariable Long id,
            @Valid @RequestBody AsignarRolRequest request) {
        return ResponseEntity.ok(usuarioService.asignarRol(id, request));
    }

    // DELETE /api/usuarios/{id}/roles/{nombreRol}
    @DeleteMapping("/{id}/roles/{nombreRol}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Revocar rol de usuario")
    public ResponseEntity<UsuarioDetalleDto> revocarRol(
            @PathVariable Long id,
            @PathVariable String nombreRol) {
        return ResponseEntity.ok(usuarioService.revocarRol(id, nombreRol));
    }

    // GET /api/usuarios/roles
    // Disponible para ADMINISTRADOR y COORDINADOR (para saber qué roles existen)
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar todos los roles disponibles")
    public ResponseEntity<List<RolDto>> listarRoles() {
        return ResponseEntity.ok(usuarioService.listarRoles());
    }

    // GET /api/usuarios/{id}/carreras
    @GetMapping("/{id}/carreras")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar carreras asignadas a un coordinador")
    public ResponseEntity<List<Long>> listarCarreras(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarCarrerasCoordinador(id));
    }

    // PUT /api/usuarios/{id}/carreras
    @PutMapping("/{id}/carreras")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar carreras asignadas a un coordinador")
    public ResponseEntity<Void> actualizarCarreras(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCarrerasRequest request) {
        usuarioService.actualizarCarrerasCoordinador(id, request.getCarreraIds());
        return ResponseEntity.noContent().build();
    }

    // GET /api/usuarios/{id}/eventos
    @GetMapping("/{id}/eventos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar eventos asignados a un auxiliar")
    public ResponseEntity<List<Long>> listarEventos(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarEventosAuxiliar(id));
    }

    // PUT /api/usuarios/{id}/eventos
    @PutMapping("/{id}/eventos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar eventos asignados a un auxiliar")
    public ResponseEntity<Void> actualizarEventos(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarEventosRequest request) {
        usuarioService.actualizarEventosAuxiliar(id, request.getEventoIds());
        return ResponseEntity.noContent().build();
    }

    // GET /api/usuarios/{id}/paralelos
    @GetMapping("/{id}/paralelos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Listar paralelos asignados a un docente")
    public ResponseEntity<List<ParaleloRefRequest>> listarParalelos(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarParalelosDocente(id));
    }

    // PUT /api/usuarios/{id}/paralelos
    @PutMapping("/{id}/paralelos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualizar paralelos asignados a un docente")
    public ResponseEntity<Void> actualizarParalelos(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarParalelosRequest request) {
        usuarioService.actualizarParalelosDocente(id, request.getParalelos());
        return ResponseEntity.noContent().build();
    }
}