package bo.edu.umsa.fhce.sistemacursos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaCursosApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaCursosApplication.class, args);
	}

}
