package bo.edu.umsa.fhce.sistemacursos.modules.evento.controller;

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

import bo.edu.umsa.fhce.sistemacursos.modules.evento.dto.AsignarAuxiliarRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.dto.AsignarDisenadorRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.dto.AuxiliarResumenDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.dto.EventoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.dto.EventoRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Gestión de eventos facultativos")
public class EventoController {

    private final EventoService eventoService;

    // GET /api/eventos — catálogo de eventos abiertos
    @GetMapping
    @PreAuthorize("permitAll()")
    @Operation(summary = "Listar eventos abiertos",
               description = "Filtra por carrera con ?idCarrera=1")
    public ResponseEntity<List<EventoDto>> listar(
            @RequestParam(required = false) Long idCarrera) {
        return ResponseEntity.ok(eventoService.listarAbiertos(idCarrera));
    }

    // GET /api/eventos/todos
    @GetMapping("/todos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar todos los eventos incluyendo finalizados")
    public ResponseEntity<List<EventoDto>> listarTodos(
            @RequestParam(required = false) Long idCarrera) {
        return ResponseEntity.ok(eventoService.listarTodos(idCarrera));
    }

    // GET /api/eventos/disenador
    @GetMapping("/disenador")
    @PreAuthorize("hasAnyRole('DISENADOR', 'DISEÑADOR')")
    @Operation(summary = "Listar eventos asignados al disenador autenticado")
    public ResponseEntity<List<EventoDto>> listarDisenador() {
        return ResponseEntity.ok(eventoService.listarAsignadosDisenador());
    }

    // GET /api/eventos/auxiliar
    @GetMapping("/auxiliar")
    @PreAuthorize("hasRole('AUXILIAR')")
    @Operation(summary = "Listar eventos asignados al auxiliar actual")
    public ResponseEntity<List<EventoDto>> listarAsignadosAuxiliar() {
        return ResponseEntity.ok(eventoService.listarAsignadosAuxiliar());
    }

    // GET /api/eventos/{id}
    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Ver detalle de un evento")
    public ResponseEntity<EventoDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtener(id));
    }

    // POST /api/eventos
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Crear evento")
    public ResponseEntity<EventoDto> crear(@Valid @RequestBody EventoRequest request) {
        return ResponseEntity.status(201).body(eventoService.crear(request));
    }

    // PUT /api/eventos/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Actualizar evento")
    public ResponseEntity<EventoDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EventoRequest request) {
        return ResponseEntity.ok(eventoService.actualizar(id, request));
    }

    // DELETE /api/eventos/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Eliminar evento")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        eventoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/eventos/{id}/estado
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Cambiar estado del evento")
    public ResponseEntity<EventoDto> cambiarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(eventoService.cambiarEstado(id, estado));
    }

    // PATCH /api/eventos/{id}/disenador
    @PatchMapping("/{id}/disenador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Asignar o remover disenador del evento")
    public ResponseEntity<EventoDto> asignarDisenador(
            @PathVariable Long id,
            @Valid @RequestBody AsignarDisenadorRequest request) {
        return ResponseEntity.ok(eventoService.asignarDisenador(id, request));
    }

    // POST /api/eventos/{id}/auxiliares
    @PostMapping("/{id}/auxiliares")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Asignar auxiliar a evento")
    public ResponseEntity<Void> asignarAuxiliar(
            @PathVariable Long id,
            @Valid @RequestBody AsignarAuxiliarRequest request) {
        eventoService.asignarAuxiliar(id, request);
        return ResponseEntity.status(201).build();
    }

    // DELETE /api/eventos/{id}/auxiliares/{idAuxiliar}
    @DeleteMapping("/{id}/auxiliares/{idAuxiliar}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Remover auxiliar de evento")
    public ResponseEntity<Void> removerAuxiliar(
            @PathVariable Long id,
            @PathVariable Long idAuxiliar) {
        eventoService.removerAuxiliar(id, idAuxiliar);
        return ResponseEntity.noContent().build();
    }

    // GET /api/eventos/{id}/auxiliares
    @GetMapping("/{id}/auxiliares")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar auxiliares asignados a un evento")
    public ResponseEntity<List<AuxiliarResumenDto>> listarAuxiliares(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.listarAuxiliares(id));
    }
}