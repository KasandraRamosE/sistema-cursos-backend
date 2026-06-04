package bo.edu.umsa.fhce.sistemacursos.modules.auth.integration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mocks.umsa-auth", havingValue = "false", matchIfMissing = true)
public class DisabledUmsaAuthClient implements UmsaAuthClient {

    @Override
    public UmsaAuthResult authenticate(String ru, String password) {
        return UmsaAuthResult.unavailable();
    }
}
