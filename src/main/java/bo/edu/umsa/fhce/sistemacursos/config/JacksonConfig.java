// src/main/java/.../config/JacksonConfig.java

package bo.edu.umsa.fhce.sistemacursos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Registrar soporte para tipos de Java 8+ de fechas:
        // LocalDate, LocalDateTime, LocalTime, ZonedDateTime, etc.
        mapper.registerModule(new JavaTimeModule());

        // Serializar fechas como String ISO-8601: "2025-05-15T10:30:00"
        // En vez del formato timestamp numérico: 1747388400000
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}