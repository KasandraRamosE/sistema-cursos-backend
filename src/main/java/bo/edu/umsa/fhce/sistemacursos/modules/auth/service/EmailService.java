package bo.edu.umsa.fhce.sistemacursos.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
@RequiredArgsConstructor
@Slf4j // Lombok genera logger: log.info(...), log.error(...), etc.
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String emailFrom;

    // @Async: el email se envía en un hilo separado para no bloquear la respuesta
    // El usuario recibe respuesta inmediata sin esperar al servidor SMTP
    @Async
    public void enviarCodigoVerificacion(String destinatario, String nombres, String codigo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(emailFrom);
            mensaje.setTo(destinatario);
            mensaje.setSubject("Verificación de cuenta — FHCE UMSA");
            mensaje.setText(construirCuerpoVerificacion(nombres, codigo));

            mailSender.send(mensaje);
            log.info("Email de verificación enviado a: {}", destinatario);

        } catch (Exception e) {
            // Loguear pero no propagar — el email fallido no debe romper el registro
            log.error("Error enviando email a {}: {}", destinatario, e.getMessage());
        }
    }

    private String construirCuerpoVerificacion(String nombres, String codigo) {
        return """
            Hola %s,

            Tu código de verificación para activar tu cuenta en el
            Sistema de Cursos Complementarios de la FHCE - UMSA es:

                %s

            Este código es válido por 24 horas.

            Si no solicitaste este registro, puedes ignorar este mensaje.

            Facultad de Humanidades y Ciencias de la Educación — UMSA
            """.formatted(nombres, codigo);
    }
}