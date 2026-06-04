package bo.edu.umsa.fhce.sistemacursos.modules.reporte.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteAcademicoDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteActividadDetalleDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteFinancieroDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.dto.ReporteParticipacionDto;
import bo.edu.umsa.fhce.sistemacursos.modules.reporte.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes academicos, financieros y estadisticas")
public class ReporteController {

    private final ReporteService reporteService;

    // GET /api/reportes/academicos
    @GetMapping("/academicos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Reporte academico de cursos, eventos e inscripciones")
    public ResponseEntity<ReporteAcademicoDto> academicos(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.reporteAcademico(idCarrera, desde, hasta));
    }

    // GET /api/reportes/financieros
    @GetMapping("/financieros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Reporte financiero por actividad y tipo de participante")
    public ResponseEntity<ReporteFinancieroDto> financieros(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.reporteFinanciero(idCarrera, desde, hasta));
    }

    // GET /api/reportes/financieros/coordinador
    @GetMapping("/financieros/coordinador")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Reporte financiero para coordinadores (limitado por carrera)")
    public ResponseEntity<ReporteFinancieroDto> financierosCoordinador(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.reporteFinancieroCoordinador(idCarrera, desde, hasta));
    }

    // GET /api/reportes/participacion
    @GetMapping("/participacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Estadisticas de participacion por carrera y tipo de usuario")
    public ResponseEntity<ReporteParticipacionDto> participacion(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteService.reporteParticipacion(idCarrera, desde, hasta));
    }

    // GET /api/reportes/actividad
    @GetMapping("/actividad")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Detalle de reporte por actividad (curso o evento)")
    public ResponseEntity<ReporteActividadDetalleDto> detalleActividad(
            @RequestParam String tipo,
            @RequestParam Long idActividad) {
        return ResponseEntity.ok(reporteService.reporteDetalleActividad(tipo, idActividad));
    }

    @GetMapping(value = "/actividad/inscritos/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Reporte PDF de inscritos por actividad (curso o evento)")
    public ResponseEntity<byte[]> inscritosPdf(
            @RequestParam String tipo,
            @RequestParam Long idActividad) {
        byte[] pdf = reporteService.reporteInscritosPdf(tipo, idActividad);

        String fileName = "reporte-inscritos-"
            + tipo.trim().toLowerCase()
            + "-"
            + idActividad
            + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdf);
    }

    @GetMapping(value = "/academicos/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Reporte académico (PDF) para coordinador con filtros")
    public ResponseEntity<byte[]> academicosPdf(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String buscar) {

        byte[] pdf = reporteService.reporteAcademicoCoordinadorPdf(desde, hasta, idCarrera, tipo, buscar);

        String fileName = "reporte-academico" + (idCarrera != null ? "-carrera-" + idCarrera : "") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @GetMapping(value = "/financieros/coordinador/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR')")
    @Operation(summary = "Reporte financiero (PDF) para coordinador con filtros")
    public ResponseEntity<byte[]> financierosCoordinadorPdf(
            @RequestParam(required = false) Long idCarrera,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String buscar) {

        byte[] pdf = reporteService.reporteFinancieroCoordinadorPdf(desde, hasta, idCarrera, tipo, buscar);

        String fileName = "reporte-financiero" + (idCarrera != null ? "-carrera-" + idCarrera : "") + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
