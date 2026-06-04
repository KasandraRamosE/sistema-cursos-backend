// src/main/java/.../modules/evaluacion/controller/AsistenciaController.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.AsistenciaAdminDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.AsistenciaDto;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.dto.RegistrarAsistenciaRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.service.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/asistencias")
@RequiredArgsConstructor
@Tag(name = "Asistencias", description = "Registro de asistencia a eventos")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    // POST /api/asistencias
    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'AUXILIAR', 'ADMINISTRADOR')")
    @Operation(summary = "Registrar asistencia de un participante a un evento")
    public ResponseEntity<AsistenciaDto> registrar(
            @Valid @RequestBody RegistrarAsistenciaRequest request) {
        return ResponseEntity.status(201)
            .body(asistenciaService.registrar(request));
    }

    // GET /api/asistencias/evento/{idEvento}
    @GetMapping("/evento/{idEvento}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'AUXILIAR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver lista de asistentes de un evento")
    public ResponseEntity<List<AsistenciaDto>> porEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(asistenciaService.asistentesDeEvento(idEvento));
    }

    // GET /api/asistencias/evento/{idEvento}/detalle
    @GetMapping("/evento/{idEvento}/detalle")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'AUXILIAR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver inscripciones de un evento con estado de asistencia")
    public ResponseEntity<List<AsistenciaAdminDto>> detalleEvento(@PathVariable Long idEvento) {
        return ResponseEntity.ok(asistenciaService.inscripcionesConAsistencia(idEvento));
    }

    // DELETE /api/asistencias/{idInscripcion}
    @DeleteMapping("/{idInscripcion}")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR', 'AUXILIAR')")
    @Operation(summary = "Anular registro de asistencia")
    public ResponseEntity<Void> anular(@PathVariable Long idInscripcion) {
        asistenciaService.anular(idInscripcion);
        return ResponseEntity.noContent().build();
    }
}