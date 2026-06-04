// src/main/java/.../modules/inscripcion/integration/LibelulaClient.java

package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.integration;

import java.math.BigDecimal;

// Interfaz que abstrae la pasarela de pagos
// En dev: MockLibelulaClient — aprueba todo automáticamente
// En prod: RealLibelulaClient — conecta a la API real
public interface LibelulaClient {

    // Inicia un pago y devuelve la referencia de transacción
    PagoResultado iniciarPago(BigDecimal monto, String descripcion);

    // Verifica el estado de una transacción
    EstadoPagoExterno verificarPago(String referenciaTransaccion);

    record PagoResultado(
        String referenciaTransaccion,
        String metodoPago,
        boolean aprobado
    ) {}

    enum EstadoPagoExterno {
        APROBADO, RECHAZADO, PENDIENTE
    }
}