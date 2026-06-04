package bo.edu.umsa.fhce.sistemacursos.modules.evento.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.carrera.entity.Carrera;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "evento")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long idEvento;

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

    @Column(name = "lugar", length = 255)
    private String lugar;

    @Column(name = "imagen", length = 255)
    private String imagen;

    @Column(name = "carga_horaria", nullable = false)
    private Integer cargaHoraria;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false, length = 10)
    private Modalidad modalidad;

    // Evento de un solo día — fecha y hora en un único campo
    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    @Column(name = "costo_externo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoExterno;

    @Column(name = "costo_umsa", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUmsa;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoEvento estado = EstadoEvento.ABIERTO;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "link", length = 255)
    private String link;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoEvento.ABIERTO;
        }
    }

    public enum Modalidad {
        PRESENCIAL, VIRTUAL, MIXTO
    }

    public enum EstadoEvento {
        ABIERTO, LLENO, FINALIZADO
    }
}