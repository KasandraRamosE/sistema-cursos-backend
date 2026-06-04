package bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "carrera")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Long idCarrera;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 8)
    private EstadoCarrera estado = EstadoCarrera.ACTIVA;

    public enum EstadoCarrera {
        ACTIVA, INACTIVA
    }
}