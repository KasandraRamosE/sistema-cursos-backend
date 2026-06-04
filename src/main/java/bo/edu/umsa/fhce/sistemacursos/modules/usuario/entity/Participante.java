package bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Perfil extendido del usuario con rol PARTICIPANTE.
// tipo_participante determina qué precio aplica en inscripciones.
@Entity
@Table(name = "participante")
@Getter @Setter
@NoArgsConstructor
public class Participante {

    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_participante", nullable = false, length = 8)
    private TipoParticipante tipoParticipante;

    public enum TipoParticipante {
        UMSA, EXTERNO
    }
}