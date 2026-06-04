// src/main/java/.../config/ModelMapperConfig.java

package bo.edu.umsa.fhce.sistemacursos.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // STRICT: los campos se mapean solo si los nombres coinciden exactamente.
        // Evita mapeos accidentales entre campos con nombres parecidos.
        mapper.getConfiguration()
            .setMatchingStrategy(MatchingStrategies.STRICT)
            // Omitir campos null al mapear (no pisa valores existentes con null)
            .setSkipNullEnabled(true);

        return mapper;
    }
}