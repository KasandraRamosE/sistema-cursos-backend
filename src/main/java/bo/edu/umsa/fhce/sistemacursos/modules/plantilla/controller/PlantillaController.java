// src/main/java/.../modules/plantilla/controller/PlantillaController.java

package bo.edu.umsa.fhce.sistemacursos.modules.plantilla.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.AprobacionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.AprobacionRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.PlantillaEstadoResumenDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.dto.PlantillaDto;
import bo.edu.umsa.fhce.sistemacursos.modules.plantilla.service.PlantillaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/plantillas")
@RequiredArgsConstructor
@Tag(name = "Plantillas", description = "Gestión de plantillas de certificados")
public class PlantillaController {

    private final PlantillaService plantillaService;

    // POST /api/plantillas
    // multipart/form-data — el archivo PDF viene como parte del formulario
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DISENADOR', 'DISEÑADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Subir plantilla PDF",
               description = "Requiere idCurso O idEvento, no ambos")
    public ResponseEntity<PlantillaDto> subir(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam(required = false) Long idCurso,
            @RequestParam(required = false) Long idEvento) throws IOException {
        return ResponseEntity.status(201)
            .body(plantillaService.subirPlantilla(archivo, idCurso, idEvento));
    }

    // GET /api/plantillas/pendientes
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver plantillas pendientes de revisión")
    public ResponseEntity<List<PlantillaDto>> pendientes() {
        return ResponseEntity.ok(plantillaService.listarPendientes());
    }

    // GET /api/plantillas/estados-por-actividad
    @GetMapping("/estados-por-actividad")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Estado más reciente de plantillas por actividad")
    public ResponseEntity<List<PlantillaEstadoResumenDto>> estadosVigentes() {
        return ResponseEntity.ok(plantillaService.estadosPorActividad());
    }

    // GET /api/plantillas/mis-plantillas
    @GetMapping("/mis-plantillas")
    @PreAuthorize("hasAnyRole('DISENADOR', 'DISEÑADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver plantillas subidas por el diseñador autenticado")
    public ResponseEntity<List<PlantillaDto>> misPlantillas() {
        return ResponseEntity.ok(plantillaService.misPlantillas());
    }

    // GET /api/plantillas/historial
    @GetMapping("/historial")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'DISENADOR', 'DISEÑADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Historial de versiones de plantillas de una actividad")
    public ResponseEntity<List<PlantillaDto>> historial(
            @RequestParam(required = false) Long idCurso,
            @RequestParam(required = false) Long idEvento) {
        if ((idCurso == null && idEvento == null)
            || (idCurso != null && idEvento != null)) {
            throw new BusinessException(
            "Debe enviar exactamente uno: idCurso o idEvento", 400);
        }
        return ResponseEntity.ok(plantillaService.historial(idCurso, idEvento));
    }

    // PATCH /api/plantillas/{id}/revisar
    @PatchMapping("/{id}/revisar")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Aprobar o rechazar plantilla",
               description = "Si se rechaza, las observaciones son obligatorias")
    public ResponseEntity<PlantillaDto> revisar(
            @PathVariable Long id,
            @Valid @RequestBody AprobacionRequest request) {
        return ResponseEntity.ok(plantillaService.revisar(id, request));
    }

    // GET /api/plantillas/{id}/descargar
    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'DISENADOR', 'DISEÑADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Descargar plantilla PDF para revisión")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) throws IOException {
        byte[] pdf = plantillaService.descargarPlantilla(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"plantilla_" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    // GET /api/plantillas/{id}/aprobaciones
    @GetMapping("/{id}/aprobaciones")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'DISENADOR', 'DISEÑADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Ver historial de revisiones de una plantilla")
    public ResponseEntity<List<AprobacionDto>> aprobaciones(@PathVariable Long id) {
        return ResponseEntity.ok(plantillaService.historialAprobaciones(id));
    }
}