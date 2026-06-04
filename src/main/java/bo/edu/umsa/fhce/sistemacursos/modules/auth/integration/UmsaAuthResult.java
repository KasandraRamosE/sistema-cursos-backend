package bo.edu.umsa.fhce.sistemacursos.modules.auth.integration;

public class UmsaAuthResult {

    private final UmsaAuthStatus status;
    private final String ru;
    private final String nombres;
    private final String apellidos;

    private UmsaAuthResult(UmsaAuthStatus status, String ru, String nombres, String apellidos) {
        this.status = status;
        this.ru = ru;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public static UmsaAuthResult success(String ru, String nombres, String apellidos) {
        return new UmsaAuthResult(UmsaAuthStatus.SUCCESS, ru, nombres, apellidos);
    }

    public static UmsaAuthResult invalidCredentials() {
        return new UmsaAuthResult(UmsaAuthStatus.INVALID_CREDENTIALS, null, null, null);
    }

    public static UmsaAuthResult notFound() {
        return new UmsaAuthResult(UmsaAuthStatus.NOT_FOUND, null, null, null);
    }

    public static UmsaAuthResult inactive() {
        return new UmsaAuthResult(UmsaAuthStatus.INACTIVE, null, null, null);
    }

    public static UmsaAuthResult unavailable() {
        return new UmsaAuthResult(UmsaAuthStatus.UNAVAILABLE, null, null, null);
    }

    public UmsaAuthStatus getStatus() {
        return status;
    }

    public String getRu() {
        return ru;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }
}
