package bo.edu.umsa.fhce.sistemacursos.modules.archivo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import bo.edu.umsa.fhce.sistemacursos.exception.BusinessException;
import lombok.Getter;

@Service
public class ArchivoStorageService {

    @Value("${app.archivos.directorio:uploads}")
    private String uploadDir;

    @Value("${app.archivos.max-size-mb:5}")
    private long maxSizeMb;

    public StoredFile storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Debes subir un archivo de imagen", 400);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen", 400);
        }

        long maxBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("La imagen excede el tamanio maximo permitido", 400);
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExtension = extension == null ? "" : "." + extension.toLowerCase();
        String filename = UUID.randomUUID() + safeExtension;

        Path baseDir = Paths.get(uploadDir, "actividades").toAbsolutePath().normalize();
        Path destination = baseDir.resolve(filename);

        try {
            Files.createDirectories(baseDir);
            file.transferTo(destination.toFile());
        } catch (IOException ex) {
            throw new BusinessException("No se pudo guardar la imagen", 500);
        }

        return new StoredFile(filename, "actividades/" + filename);
    }

    @Getter
    public static class StoredFile {
        private final String filename;
        private final String relativePath;

        public StoredFile(String filename, String relativePath) {
            this.filename = filename;
            this.relativePath = relativePath;
        }
    }
}
