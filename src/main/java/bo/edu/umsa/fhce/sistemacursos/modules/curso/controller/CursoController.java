// src/main/java/.../modules/curso/controller/CursoController.java

package bo.edu.umsa.fhce.sistemacursos.modules.curso.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.curso.service.CursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
@Tag(name = "Cursos", description = "Gestión de cursos complementarios y sus paralelos")
public class CursoController {

    private final CursoService cursoService;

    // GET /api/cursos — catálogo de cursos abiertos
    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Listar cursos abiertos",
               description = "Filtra por carrera con ?idCarrera=1")
    public ResponseEntity<List<CursoDto>> listar(
            @RequestParam(required = false) Long idCarrera) {
        return ResponseEntity.ok(cursoService.listarAbiertos(idCarrera));
    }

    // GET /api/cursos/todos — para admin y coordinador
    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar todos los cursos incluyendo finalizados")
    public ResponseEntity<List<CursoDto>> listarTodos(
            @RequestParam(required = false) Long idCarrera) {
        return ResponseEntity.ok(cursoService.listarTodos(idCarrera));
    }

    // GET /api/cursos/disenador
    @GetMapping("/disenador")
    @PreAuthorize("hasAnyRole('DISENADOR', 'DISEÑADOR')")
    @Operation(summary = "Listar cursos asignados al disenador autenticado")
    public ResponseEntity<List<CursoDto>> listarDisenador() {
        return ResponseEntity.ok(cursoService.listarAsignadosDisenador());
    }

    // GET /api/cursos/{id}
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Ver detalle de un curso con sus paralelos")
    public ResponseEntity<CursoDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(cursoService.obtener(id));
    }

    // POST /api/cursos
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Crear curso")
    public ResponseEntity<CursoDto> crear(@Valid @RequestBody CursoRequest request) {
        return ResponseEntity.status(201).body(cursoService.crear(request));
    }

    // PUT /api/cursos/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Actualizar curso")
    public ResponseEntity<CursoDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.actualizar(id, request));
    }

    // DELETE /api/cursos/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Eliminar curso")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/cursos/{id}/estado
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Cambiar estado del curso")
    public ResponseEntity<CursoDto> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(cursoService.cambiarEstado(id, estado));
    }

    // PATCH /api/cursos/{id}/disenador
    @PatchMapping("/{id}/disenador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Asignar o remover disenador del curso")
    public ResponseEntity<CursoDto> asignarDisenador(
            @PathVariable Long id,
            @Valid @RequestBody AsignarDisenadorRequest request) {
        return ResponseEntity.ok(cursoService.asignarDisenador(id, request));
    }

    // ── Endpoints de paralelos ────────────────────────────────────────────────

    // POST /api/cursos/{id}/paralelos
    @PostMapping("/{id}/paralelos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Agregar paralelo al curso")
    public ResponseEntity<ParaleloDto> agregarParalelo(
            @PathVariable Long id,
            @Valid @RequestBody ParaleloRequest request) {
        return ResponseEntity.status(201).body(cursoService.agregarParalelo(id, request));
    }

    // PUT /api/cursos/{id}/paralelos/{codigo}
    @PutMapping("/{id}/paralelos/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Actualizar paralelo")
    public ResponseEntity<ParaleloDto> actualizarParalelo(
            @PathVariable Long id,
            @PathVariable String codigo,
            @Valid @RequestBody ParaleloRequest request) {
        return ResponseEntity.ok(cursoService.actualizarParalelo(id, codigo, request));
    }

    // DELETE /api/cursos/{id}/paralelos/{codigo}
    @DeleteMapping("/{id}/paralelos/{codigo}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Eliminar paralelo")
    public ResponseEntity<Void> eliminarParalelo(
            @PathVariable Long id,
            @PathVariable String codigo) {
        cursoService.eliminarParalelo(id, codigo);
        return ResponseEntity.noContent().build();
    }
}