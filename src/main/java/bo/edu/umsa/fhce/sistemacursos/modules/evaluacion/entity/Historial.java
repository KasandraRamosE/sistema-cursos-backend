// src/main/java/.../modules/evaluacion/entity/Historial.java

package bo.edu.umsa.fhce.sistemacursos.modules.evaluacion.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Auditoría de cambios de nota — cada modificación genera un nuevo registro
@Entity
@Table(name = "historial")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Historial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_evaluacion", nullable = false)
    private EvaluacionEstudiante evaluacion;

    @Column(name = "nota_anterior", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaAnterior;

    @Column(name = "nota_nueva", nullable = false, precision = 5, scale = 2)
    private BigDecimal notaNueva;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    // Solo el administrador puede modificar notas
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cambiado_por", nullable = false)
    private Usuario cambiadoPor;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @PrePersist
    protected void onCreate() {
        this.fechaCambio = LocalDateTime.now();
    }
}