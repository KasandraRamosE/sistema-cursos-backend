// src/main/java/.../modules/evaluacion/controller/EvaluacionController.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.ParaleloEstadoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity.Historial;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.service.EvaluacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
@Tag(name = "Evaluaciones", description = "Registro de notas y solicitudes de certificados")
public class EvaluacionController {

    private final EvaluacionService evaluacionService;

    // POST /api/evaluaciones
    @PostMapping
    @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar nota a un participante")
    public ResponseEntity<EvaluacionDto> registrar(
            @Valid @RequestBody RegistrarNotaRequest request) {
        return ResponseEntity.status(201)
            .body(evaluacionService.registrarNota(request));
    }

    // POST /api/evaluaciones/lote
    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('DOCENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar notas en lote para un paralelo")
    public ResponseEntity<List<EvaluacionDto>> registrarLote(
            @Valid @RequestBody List<RegistrarNotaRequest> requests) {
        return ResponseEntity.status(201)
            .body(evaluacionService.registrarNotasLote(requests));
    }

    // GET /api/evaluaciones/paralelo/{idCurso}/{codigo}
    @GetMapping("/paralelo/{idCurso}/{codigo}")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver notas de un paralelo")
    public ResponseEntity<List<EvaluacionDto>> porParalelo(
            @PathVariable Long idCurso,
            @PathVariable String codigo) {
        return ResponseEntity.ok(evaluacionService.notasDeParalelo(idCurso, codigo));
    }

    // GET /api/evaluaciones/paralelo/{idCurso}/{codigo}/estado
    @GetMapping("/paralelo/{idCurso}/{codigo}/estado")
    @PreAuthorize("hasAnyRole('DOCENTE', 'COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Estado de confirmacion del paralelo")
    public ResponseEntity<ParaleloEstadoDto> estadoParalelo(
            @PathVariable Long idCurso,
            @PathVariable String codigo) {
        ParaleloEstadoDto dto = new ParaleloEstadoDto();
        dto.setConfirmado(evaluacionService.paraleloConfirmado(idCurso, codigo));
        return ResponseEntity.ok(dto);
    }

    // PATCH /api/evaluaciones/{id}/nota
    @PatchMapping("/{id}/nota")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Modificar nota — solo administrador")
    public ResponseEntity<EvaluacionDto> modificar(
            @PathVariable Long id,
            @Valid @RequestBody ModificarNotaRequest request) {
        return ResponseEntity.ok(evaluacionService.modificarNota(id, request));
    }

    // GET /api/evaluaciones/{id}/historial
    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Ver historial de cambios de una evaluación")
    public ResponseEntity<List<Historial>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(evaluacionService.historialDeEvaluacion(id));
    }

    // POST /api/evaluaciones/confirmar/{idCurso}
    @PostMapping("/confirmar/{idCurso}")
    @PreAuthorize("hasRole('DOCENTE')")
    @Operation(summary = "Confirmar notas de un paralelo y generar solicitud de emisión")
    public ResponseEntity<SolicitudEmisionDto> confirmar(
            @PathVariable Long idCurso,
            @Valid @RequestBody ConfirmarNotasRequest request) {
        return ResponseEntity.status(201)
            .body(evaluacionService.confirmarNotas(idCurso, request));
    }

    // GET /api/evaluaciones/solicitudes
    @GetMapping("/solicitudes")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver solicitudes de emisión pendientes")
    public ResponseEntity<List<SolicitudEmisionDto>> solicitudes() {
        return ResponseEntity.ok(evaluacionService.solicitudesPendientes());
    }

    // GET /api/evaluaciones/solicitudes/todas
    @GetMapping("/solicitudes/todas")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver historial de solicitudes de emisión")
    public ResponseEntity<List<SolicitudEmisionDto>> solicitudesTodas() {
        return ResponseEntity.ok(evaluacionService.solicitudesTodas());
    }

    // POST /api/evaluaciones/solicitudes/evento/{idEvento}
    @PostMapping("/solicitudes/evento/{idEvento}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Crear solicitud de emisión para un evento")
    public ResponseEntity<SolicitudEmisionDto> solicitarEvento(
            @PathVariable Long idEvento,
            @Valid @RequestBody SolicitudEventoRequest request) {
        return ResponseEntity.status(201)
            .body(evaluacionService.solicitarEmisionEvento(idEvento, request));
    }

    // PATCH /api/evaluaciones/solicitudes/{id}
    @PatchMapping("/solicitudes/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Procesar solicitud de emisión")
    public ResponseEntity<SolicitudEmisionDto> procesarSolicitud(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(evaluacionService.procesarSolicitud(id, estado));
    }
}