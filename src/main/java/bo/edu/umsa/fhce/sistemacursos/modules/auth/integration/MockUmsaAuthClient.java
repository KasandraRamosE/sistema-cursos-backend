package bo.edu.umsa.fhce.sistemacursos.modules.auth.integration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mocks.umsa-auth", havingValue = "true")
public class MockUmsaAuthClient implements UmsaAuthClient {

    private static class MockUser {
        private final String ru;
        private final String password;
        private final String nombres;
        private final String apellidos;
        private final boolean activo;

        private MockUser(String ru, String password, String nombres, String apellidos, boolean activo) {
            this.ru = ru;
            this.password = password;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.activo = activo;
        }
    }

    private final Map<String, MockUser> users = new HashMap<>();

    public MockUmsaAuthClient() {
        users.put("20190001", new MockUser("20190001", "umsa123", "Mariana", "Lopez", true));
        users.put("20190002", new MockUser("20190002", "umsa123", "Diego", "Rojas", true));
        users.put("20190003", new MockUser("20190003", "umsa123", "Valeria", "Quispe", false));
    }

    @Override
    public UmsaAuthResult authenticate(String ru, String password) {
        MockUser user = users.get(ru);
        if (user == null) {
            return UmsaAuthResult.notFound();
        }
        if (!user.activo) {
            return UmsaAuthResult.inactive();
        }
        if (!user.password.equals(password)) {
            return UmsaAuthResult.invalidCredentials();
        }
        return UmsaAuthResult.success(user.ru, user.nombres, user.apellidos);
    }
}
