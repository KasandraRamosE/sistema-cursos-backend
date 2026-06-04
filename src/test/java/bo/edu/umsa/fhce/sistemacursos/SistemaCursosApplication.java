package bo.edu.umsa.fhce.sistemacursos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.SpringApplication;

@SpringBootTest
class SistemaCursosApplicationTests {

	@Test
	void contextLoads() {
	}

}
// SistemaCursosApplication.java

@SpringBootApplication
@EnableAsync // ← agregar esta anotación
public class SistemaCursosApplication {
    public static void main(String[] args) {
        SpringApplication.run(SistemaCursosApplication.class, args);
    }
}
