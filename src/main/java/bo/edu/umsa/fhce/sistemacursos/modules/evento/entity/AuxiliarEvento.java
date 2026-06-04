package bo.edu.umsa.fhce.sistemacursos.modules.evento.entity;

import java.time.LocalDateTime;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auxiliar_evento")
@Getter @Setter
@NoArgsConstructor
public class AuxiliarEvento {

    @EmbeddedId
    private AuxiliarEventoId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idAuxiliar")
    @JoinColumn(name = "id_auxiliar")
    private Usuario auxiliar;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("idEvento")
    @JoinColumn(name = "id_evento")
    private Evento evento;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDateTime fechaAsignacion;

    @PrePersist
    protected void onCreate() {
        this.fechaAsignacion = LocalDateTime.now();
    }

    public AuxiliarEvento(Usuario auxiliar, Evento evento) {
        this.auxiliar = auxiliar;
        this.evento   = evento;
        this.id       = new AuxiliarEventoId(
            auxiliar.getIdUsuario(),
            evento.getIdEvento()
        );
    }
}