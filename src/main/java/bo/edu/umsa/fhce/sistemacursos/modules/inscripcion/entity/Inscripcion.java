package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.curso.entity.Curso;
import bo.edu.umsa.fhce.sistemacursos.modules.evento.entity.Evento;
import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscripcion")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inscripcion")
    private Long idInscripcion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_participante", nullable = false)
    private Usuario participante;

    // Exactamente uno de estos dos tiene valor — el otro es null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso")
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evento")
    private Evento evento;

    // Solo tiene valor cuando es inscripción a curso
    @Column(name = "codigo_paralelo", length = 10)
    private String codigoParalelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_precio", nullable = false, length = 8)
    private TipoPrecio tipoPrecio;

    // Monto que debe pagar el participante
    @Column(name = "saldo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo;

    @Column(name = "fecha_inscripcion", nullable = false, updatable = false)
    private LocalDateTime fechaInscripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoInscripcion estado = EstadoInscripcion.PENDIENTE;

    @PrePersist
    protected void onCreate() {
        this.fechaInscripcion = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoInscripcion.PENDIENTE;
        }
    }

    public enum TipoPrecio {
        UMSA, EXTERNO
    }

    public enum EstadoInscripcion {
        PENDIENTE, CONFIRMADA, CANCELADA
    }
}