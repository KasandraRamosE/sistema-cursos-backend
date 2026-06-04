package bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.integration;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

// Solo se activa con el perfil "dev"
// En prod Spring inyecta RealLibelulaClient en su lugar
@Component
@Profile("dev")
@Slf4j
public class MockLibelulaClient implements LibelulaClient {

    @Override
    public PagoResultado iniciarPago(BigDecimal monto, String descripcion) {
        // Genera una referencia falsa pero con formato realista
        String referencia = "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[MOCK Libélula] Pago iniciado — monto: {} — ref: {}", monto, referencia);

        // En dev simulamos que el pago queda pendiente hasta confirmación
        return new PagoResultado(referencia, "MOCK_QR", false);
    }

    @Override
    public EstadoPagoExterno verificarPago(String referenciaTransaccion) {
        log.info("[MOCK Libélula] Verificando pago: {}", referenciaTransaccion);
        // En dev simulamos verificacion pendiente por defecto
        return EstadoPagoExterno.PENDIENTE;
    }
}