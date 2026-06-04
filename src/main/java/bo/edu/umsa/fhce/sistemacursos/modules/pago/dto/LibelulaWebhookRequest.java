package bo.edu.umsa.fhce.sistemacursos.modules.pago.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class LibelulaWebhookRequest {
    // Identificador de transacción en la pasarela
    private String transactionId;

    // Referencia enviada por el merchant (podría contener idInscripcion)
    private String merchantReference;

    private BigDecimal amount;
    private String currency;

    // Estado enviado por Libélula (ej: PAID, APPROVED, REJECTED)
    private String status;

    // Timestamp en ISO8601 o epoch
    private String timestamp;

    // Firma HMAC del payload (si llega en el body también)
    private String signature;
}
