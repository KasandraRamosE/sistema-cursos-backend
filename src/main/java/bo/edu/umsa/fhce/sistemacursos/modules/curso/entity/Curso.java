package bo.edu.umsa.fhce.sistemacursos.modules.curso.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.Carrera;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curso")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Long idCurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organizador", nullable = false)
    private Usuario organizador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_disenador")
    private Usuario disenador;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "imagen", length = 255)
    private String imagen;

    @Column(name = "carga_horaria", nullable = false)
    private Integer cargaHoraria;

    @Column(name = "duracion")
    private Integer duracion;

    @Column(name = "unidad", length = 10)
    private String unidad;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "costo_externo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoExterno;

    @Column(name = "costo_umsa", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUmsa;

    @Column(name = "nota_aprobacion", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaAprobacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoCurso estado = EstadoCurso.ABIERTO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // Un curso tiene uno o más paralelos
    // CascadeType.ALL: si eliminamos el curso, se eliminan sus paralelos
    // orphanRemoval: si removemos un paralelo de la lista, se elimina de la BD
    @Builder.Default
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Paralelo> paralelos = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoCurso.ABIERTO;
        }
    }

    public enum EstadoCurso {
        ABIERTO, LLENO, FINALIZADO
    }
}