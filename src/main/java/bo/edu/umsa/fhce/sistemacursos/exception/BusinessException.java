package bo.edu.umsa.fhce.sistemacursos.exception;

public class BusinessException extends RuntimeException {

    private final int httpStatus; // código HTTP a devolver

    public BusinessException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    // Constructor rápido para el caso más común: 422 Unprocessable Entity
    public BusinessException(String message) {
        this(message, 422);
    }

    public int getHttpStatus() { return httpStatus; }
}