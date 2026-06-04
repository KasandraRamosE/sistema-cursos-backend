package bo.edu.umsa.fhce.sistemacursos.modules.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto.AdminDashboardDto;
import bo.edu.umsa.fhce.sistemacursos.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Resumenes del sistema")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Resumen del dashboard para administradores")
    public ResponseEntity<AdminDashboardDto> adminDashboard() {
        return ResponseEntity.ok(dashboardService.obtenerDashboardAdmin());
    }
}
