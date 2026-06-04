package bo.edu.umsa.fhce.sistemacursos.exception;

public class ResourceNotFoundException extends RuntimeException {

    // Nombre del recurso y el identificador para mensajes claros:
    // "Usuario no encontrado con id: 5"
    private final String resourceName;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        // Mensaje que verá el desarrollador en los logs
        super(resourceName + " no encontrado con id: " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public String getResourceName() { return resourceName; }
    public Object getResourceId()   { return resourceId; }
}