package bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DashboardStatsDto {
    private long totalUsuarios;
    private long usuariosInternos;
    private long usuariosExternos;
    private long totalActividades;
    private long cursosActivos;
    private long eventosActivos;
    private long totalInscripciones;
    private long inscripcionesActivas;
    private long totalCertificados;
    private long certificadosPendientes;
}
