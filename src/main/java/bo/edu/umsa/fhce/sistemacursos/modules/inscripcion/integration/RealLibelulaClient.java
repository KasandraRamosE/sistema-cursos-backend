package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

// Esqueleto para producción — se implementa cuando UMSA provea acceso a Libélula
@Component
@Profile("prod")
public class RealLibelulaClient implements LibelulaClient {

    @Override
    public PagoResultado iniciarPago(BigDecimal monto, String descripcion) {
        // TODO: implementar llamada real a API de Libélula
        throw new UnsupportedOperationException(
            "Integración real con Libélula pendiente de implementación");
    }

    @Override
    public EstadoPagoExterno verificarPago(String referenciaTransaccion) {
        // TODO: implementar verificación real
        throw new UnsupportedOperationException(
            "Integración real con Libélula pendiente de implementación");
    }
}