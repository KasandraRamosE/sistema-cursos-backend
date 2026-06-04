// src/main/java/.../modules/certificado/controller/CertificadoController.java

package bo.edu.umsa.fhce.sistemacursos.modules.certificado.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.certificado.dto.*;
import bo.edu.umsa.fhce.sistemacursos.modules.certificado.service.CertificadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/certificados")
@RequiredArgsConstructor
@Tag(name = "Certificados", description = "Emisión, descarga y verificación de certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    // POST /api/certificados
    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Emitir certificado individual")
    public ResponseEntity<CertificadoDto> emitir(
            @Valid @RequestBody EmitirCertificadoRequest request) {
        return ResponseEntity.status(201)
            .body(certificadoService.emitir(request));
    }

    // POST /api/certificados/lote
    @PostMapping("/lote")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Emitir certificados en lote para un paralelo")
    public ResponseEntity<List<CertificadoDto>> emitirLote(
            @Valid @RequestBody EmitirLoteRequest request) {
        return ResponseEntity.status(201)
            .body(certificadoService.emitirLote(request));
    }

    // GET /api/certificados/mis-certificados
    @GetMapping("/mis-certificados")
    @PreAuthorize("hasRole('PARTICIPANTE')")
    @Operation(summary = "Ver mis certificados")
    public ResponseEntity<List<CertificadoDto>> misCertificados() {
        return ResponseEntity.ok(certificadoService.misCertificados());
    }

    // GET /api/certificados/admin
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Listar certificados (admin/coordinador)")
    public ResponseEntity<List<CertificadoDto>> listarTodos() {
        return ResponseEntity.ok(certificadoService.listarTodos());
    }

    // GET /api/certificados/{id}/descargar
    // Devuelve el PDF como bytes para descarga directa
    @GetMapping("/{id}/descargar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Descargar certificado en PDF")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        byte[] pdfBytes = certificadoService.descargar(id);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"certificado_" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfBytes);
    }

    // PATCH /api/certificados/{id}/anular
    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasAnyRole('COORDINADOR', 'ADMINISTRADOR')")
    @Operation(summary = "Anular certificado (con opción de reemitir)")
    public ResponseEntity<CertificadoDto> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularCertificadoRequest request) {
        return ResponseEntity.ok(certificadoService.anular(id, request));
    }

    // GET /api/verificar/{codigo}
    // Endpoint PÚBLICO — sin autenticación — para escanear QR
    @GetMapping("/verificar/{codigo}")
    @Operation(summary = "Verificar certificado por código QR — acceso público")
    public ResponseEntity<VerificacionDto> verificar(
            @PathVariable String codigo) {
        return ResponseEntity.ok(certificadoService.verificar(codigo));
    }
}