package bo.edu.umsa.fhce.sistemacursos.modules.pago.controller;

import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.dto.PagoConfirmarRequest;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.entity.Pago;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.repository.PagoRepository;
import bo.edu.umsa.fhce.sistemacursos.modules.inscripcion.service.InscripcionService;
import bo.edu.umsa.fhce.sistemacursos.modules.pago.dto.LibelulaWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/payments/webhook/libelula")
@RequiredArgsConstructor
@Slf4j
public class LibelulaWebhookController {

    private final PagoRepository pagoRepository;
    private final InscripcionService inscripcionService;

    @Value("${app.libelula.webhook-secret:}")
    private String webhookSecret;

    @PostMapping
    public ResponseEntity<?> handleWebhook(@RequestBody LibelulaWebhookRequest payload,
                                           @RequestHeader(value = "X-Signature", required = false) String headerSignature) {

        log.info("Webhook recibido de Libélula: tx={} merchant={} status={}",
            payload.getTransactionId(), payload.getMerchantReference(), payload.getStatus());

        // Validar firma si hay secreto configurado
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            String expected = computeSignature(payload);
            String provided = headerSignature != null ? headerSignature : payload.getSignature();
            if (provided == null || !provided.equals(expected)) {
                log.warn("Firma inválida para webhook Libélula: expected={} provided={}", expected, provided);
                return ResponseEntity.status(400).body("Invalid signature");
            }
        }

        // Buscar pago por referencia de transacción
        Optional<Pago> pagoOpt = Optional.empty();
        if (payload.getTransactionId() != null) {
            pagoOpt = pagoRepository.findByReferenciaTransaccion(payload.getTransactionId());
        }

        // Si no se encontró, intentar usar merchantReference como idInscripcion
        if (pagoOpt.isEmpty() && payload.getMerchantReference() != null) {
            try {
                Long idInscripcion = Long.parseLong(payload.getMerchantReference());
                pagoOpt = pagoRepository.findByInscripcion_IdInscripcion(idInscripcion);
            } catch (NumberFormatException ex) {
                log.debug("merchantReference no es un id numérico: {}", payload.getMerchantReference());
            }
        }

        if (pagoOpt.isEmpty()) {
            log.warn("Pago no encontrado para webhook: tx={} merchant={}", payload.getTransactionId(), payload.getMerchantReference());
            return ResponseEntity.status(404).body("Pago not found");
        }

        Pago pago = pagoOpt.get();

        // Idempotencia: si ya está aprobado, devolver 200
        if (pago.getEstado() == Pago.EstadoPago.APROBADO) {
            log.info("Webhook recibido para pago ya aprobado: {}", pago.getIdPago());
            return ResponseEntity.ok().build();
        }

        // Aceptar solo estados conocidos como aprobados
        String st = payload.getStatus() == null ? "" : payload.getStatus().toUpperCase();
        if (st.equals("PAID") || st.equals("APPROVED") || st.equals("COMPLETED")) {
            // Confirmar pago usando servicio (usa la lógica existente)
            PagoConfirmarRequest req = new PagoConfirmarRequest();
            req.setIdInscripcion(pago.getInscripcion().getIdInscripcion());
            inscripcionService.confirmarPago(req);
            log.info("Pago confirmado vía webhook: pagoId={} inscripcion={}", pago.getIdPago(), pago.getInscripcion().getIdInscripcion());
            return ResponseEntity.ok().build();
        } else if (st.equals("REJECTED") || st.equals("FAILED")) {
            // Marcar rechazo si es necesario (simplificado)
            pago.setEstado(Pago.EstadoPago.RECHAZADO);
            // Guardar directamente en repositorio
            pagoRepository.save(pago);
            log.info("Pago marcado como RECHAZADO vía webhook: pagoId={}", pago.getIdPago());
            return ResponseEntity.ok().build();
        }

        log.info("Webhook Libélula con estado no procesable: {}", payload.getStatus());
        return ResponseEntity.ok().build();
    }

    private String computeSignature(LibelulaWebhookRequest payload) {
        // Concatenar campos en orden definido para validar firma
        String base = (payload.getTransactionId() == null ? "" : payload.getTransactionId()) + "|"
            + (payload.getMerchantReference() == null ? "" : payload.getMerchantReference()) + "|"
            + (payload.getAmount() == null ? "" : payload.getAmount().toPlainString()) + "|"
            + (payload.getCurrency() == null ? "" : payload.getCurrency()) + "|"
            + (payload.getStatus() == null ? "" : payload.getStatus()) + "|"
            + (payload.getTimestamp() == null ? "" : payload.getTimestamp());
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] mac = hmac.doFinal(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac);
        } catch (Exception ex) {
            log.error("Error computing HMAC signature", ex);
            return "";
        }
    }
}
