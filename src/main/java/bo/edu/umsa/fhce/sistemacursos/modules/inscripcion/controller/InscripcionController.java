package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.service.InscripcionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inscripciones")
@RequiredArgsConstructor
@Tag(name = "Inscripciones", description = "Inscripciones y pagos de cursos y eventos")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    // POST /api/inscripciones
    @PostMapping
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @Operation(summary = "Inscribirse a un curso o evento")
    public ResponseEntity<InscripcionDto> inscribirse(
            @Valid @RequestBody InscripcionRequest request) {
        return ResponseEntity.status(201)
            .body(inscripcionService.inscribirse(request));
    }

    // POST /api/inscripciones/pago
    @PostMapping("/pago")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @Operation(summary = "Iniciar pago de una inscripción mediante Libélula")
    public ResponseEntity<PagoDto> iniciarPago(
            @Valid @RequestBody PagoIniciarRequest request) {
        return ResponseEntity.ok(inscripcionService.iniciarPago(request));
    }

    // POST /api/inscripciones/pago/confirmar
    @PostMapping("/pago/confirmar")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @Operation(summary = "Confirmar pago de una inscripción (simulado / webhook)")
    public ResponseEntity<PagoDto> confirmarPago(
            @Valid @RequestBody PagoConfirmarRequest request) {
        return ResponseEntity.ok(inscripcionService.confirmarPago(request));
    }

    // GET /api/inscripciones/mis-inscripciones
    @GetMapping("/mis-inscripciones")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @Operation(summary = "Ver mis inscripciones")
    public ResponseEntity<List<InscripcionDto>> misInscripciones() {
        return ResponseEntity.ok(inscripcionService.misInscripciones());
    }

    // PATCH /api/inscripciones/{id}/cancelar
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PARTICIPANTE', 'ADMINISTRADOR')")
    @Operation(summary = "Cancelar inscripción")
    public ResponseEntity<InscripcionDto> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(inscripcionService.cancelar(id));
    }

    // GET /api/inscripciones/curso/{idCurso}
    @GetMapping("/curso/{idCurso}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR', 'DOCENTE')")
    @Operation(summary = "Ver inscripciones de un curso")
    public ResponseEntity<List<InscripcionDto>> porCurso(@PathVariable Long idCurso) {
        return ResponseEntity.ok(inscripcionService.inscripcionesDeCurso(idCurso));
    }

    // GET /api/inscripciones/evento/{idEvento}
    @GetMapping("/evento/{idEvento}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR', 'AUXILIAR')")
    @Operation(summary = "Ver inscripciones de un evento")
    public ResponseEntity<List<InscripcionDto>> porEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(inscripcionService.inscripcionesDeEvento(idEvento));
    }
}