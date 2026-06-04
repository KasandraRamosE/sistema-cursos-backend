package bo.edu.umsa.fhce.sistemacursos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// @RestControllerAdvice intercepta TODAS las excepciones lanzadas desde
// cualquier controller y las convierte en respuestas JSON estructuradas.
// Sin esto, Spring devuelve páginas HTML de error — inaceptable para una API REST.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Estructura estándar de error que devolvemos ──────────────────────────
    // Todos los errores tienen este mismo formato JSON:
    // {
    //   "timestamp": "2025-05-15T10:30:00",
    //   "status": 404,
    //   "error": "Not Found",
    //   "message": "Usuario no encontrado con id: 5",
    //   "path": null
    // }
    private Map<String, Object> buildError(int status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    // ── 404: recurso no encontrado ───────────────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(buildError(404, "Not Found", ex.getMessage()));
    }

    // ── 409/422: regla de negocio violada ───────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(buildError(ex.getHttpStatus(), "Business Rule Violation", ex.getMessage()));
    }

    // ── 400: errores de validación en DTOs (@Valid) ──────────────────────────
    // Ejemplo: mandar email vacío en LoginRequest → devuelve qué campos fallaron
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // Construimos un mapa campo → mensaje de error
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = buildError(400, "Validation Failed", "Error en los datos enviados");
        body.put("fieldErrors", fieldErrors); // campo extra con el detalle
        return ResponseEntity.badRequest().body(body);
    }

    // ── 401: credenciales inválidas ──────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(buildError(401, "Unauthorized", "Credenciales inválidas"));
    }

    // ── 403: sin permisos para el recurso ───────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(buildError(403, "Forbidden", "No tienes permisos para realizar esta acción"));
    }

    // ── 405: método HTTP no soportado ───────────────────────────────────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = "Método HTTP no soportado para este endpoint";
        if (ex.getMethod() != null) {
            message = message + ": " + ex.getMethod();
        }
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(buildError(405, "Method Not Allowed", message));
    }

    // ── 500: cualquier error no esperado ────────────────────────────────────
    // Este es el catch-all: si algo explota y no lo capturamos antes,
    // el usuario recibe 500 en vez de un stack trace completo.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Log completo en servidor, mensaje genérico al cliente
        ex.printStackTrace();
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildError(500, "Internal Server Error",
                "Error interno del servidor. Contacta al administrador."));
    }
}