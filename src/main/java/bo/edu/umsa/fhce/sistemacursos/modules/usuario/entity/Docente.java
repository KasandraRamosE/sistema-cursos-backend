package bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Perfil extendido del usuario con rol DOCENTE.
// Relación 1:1 con Usuario — comparte la misma PK (id_usuario).
@Entity
@Table(name = "docente")
@Getter @Setter
@NoArgsConstructor
public class Docente {

    // @MapsId: usa el id del Usuario como PK de Docente.
    // No hay columna id propia — id_usuario es la PK y la FK.
    @Id
    @Column(name = "id_usuario")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    // Lic., MSc., PhD., etc.
    @Column(name = "titulo", nullable = false, length = 50)
    private String titulo;
}