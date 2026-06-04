package bo.edu.umsa.fhce.sistemacursos.modules.archivo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import bo.edu.umsa.fhce.sistemacursos.modules.archivo.dto.UploadResponse;
import bo.edu.umsa.fhce.sistemacursos.modules.archivo.service.ArchivoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/archivos")
@RequiredArgsConstructor
@Tag(name = "Archivos", description = "Carga de archivos del sistema")
public class ArchivoController {

    private final ArchivoStorageService storageService;

    @PostMapping("/imagenes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','COORDINADOR')")
    @Operation(summary = "Subir imagen para actividad")
    public ResponseEntity<UploadResponse> subirImagen(@RequestParam("archivo") MultipartFile archivo) {
        ArchivoStorageService.StoredFile stored = storageService.storeImage(archivo);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/")
            .path(stored.getRelativePath())
            .toUriString();

        UploadResponse response = new UploadResponse();
        response.setFilename(stored.getFilename());
        response.setUrl(url);

        return ResponseEntity.ok(response);
    }
}
