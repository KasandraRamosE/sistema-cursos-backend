package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Long idPago;

    // Relación 1:1 con inscripción — cada inscripción tiene a lo sumo un pago
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_inscripcion", nullable = false)
    private Inscripcion inscripcion;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    // Método usado en pasarela Libélula: tarjeta, QR, etc.
    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    // ID de transacción devuelto por Libélula
    @Column(name = "referencia_transaccion", length = 150)
    private String referenciaTransaccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private EstadoPago estado = EstadoPago.PENDIENTE;

    // Se setea cuando Libélula confirma el pago
    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoPago.PENDIENTE;
        }
    }

    public enum EstadoPago {
        PENDIENTE, APROBADO, RECHAZADO
    }
}