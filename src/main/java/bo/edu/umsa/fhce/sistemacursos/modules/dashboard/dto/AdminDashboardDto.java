package bo.edu.umsa.fhce.sistemacursos.modules.dashboard.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDashboardDto {
    private DashboardStatsDto stats;
    private List<DashboardActividadDto> actividadesRecientes;
}
