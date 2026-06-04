package bo.edu.umsa.fhce.sistemacursos.modules.carrera.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.edu.umsa.fhce.sistemacursos.modules.carrera.dto.CarreraDto;
import bo.edu.umsa.fhce.sistemacursos.modules.carrera.service.CarreraService;
import bo.edu.umsa.fhce.sistemacursos.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coordinador")
@RequiredArgsConstructor
@Tag(name = "Coordinador", description = "Recursos del coordinador")
public class CoordinadorCarreraController {

    private final CarreraService carreraService;

    // GET /api/coordinador/carreras
    @GetMapping("/carreras")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Listar carreras del coordinador autenticado")
    public ResponseEntity<List<CarreraDto>> misCarreras() {
        CustomUserDetails userDetails = (CustomUserDetails)
            SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long idCoordinador = userDetails.getIdUsuario();
        return ResponseEntity.ok(carreraService.listarCarrerasDeCoordinador(idCoordinador));
    }
}
