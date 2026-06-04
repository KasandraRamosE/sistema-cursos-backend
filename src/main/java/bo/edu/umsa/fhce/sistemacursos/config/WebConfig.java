package bo.edu.umsa.fhce.sistemacursos.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.archivos.directorio:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadsPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = uploadsPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
            .addResourceLocations(location);
    }
}
