package bo.edu.umsa.fhce.sistemacursos.modules.auth.integration;

public interface UmsaAuthClient {
    UmsaAuthResult authenticate(String ru, String password);
}
