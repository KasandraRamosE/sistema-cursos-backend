package bo.edu.umsa.fhce.sistemacursos.modules.auth.dto;

public class RefreshTokenResponse {
    private String token;
    private String tipo;
    private String refreshToken;

    public RefreshTokenResponse() {
    }

    public RefreshTokenResponse(String token, String tipo, String refreshToken) {
        this.token = token;
        this.tipo = tipo;
        this.refreshToken = refreshToken;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
