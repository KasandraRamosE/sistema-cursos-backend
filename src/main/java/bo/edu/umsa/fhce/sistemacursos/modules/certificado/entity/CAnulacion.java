package bo.edu.umsa.fhce.sistemacursos.modules.certificado.entity;

import bo.edu.umsa.fhce.sistemacursos.modules.usuario.entity.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "c_anulacion")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CAnulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anulacion")
    private Long idAnulacion;

    // 1:1 con certificado — un certificado solo se anula una vez
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_certificado", nullable = false, unique = true)
    private Certificado certificado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "motivo_anulacion", nullable = false, columnDefinition = "TEXT")
    private String motivoAnulacion;

    // Null si solo se anuló sin reemitir
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_certificado_reemplazo")
    private Certificado certificadoReemplazo;

    @Column(name = "fecha_anulacion", nullable = false, updatable = false)
    private LocalDateTime fechaAnulacion;

    @PrePersist
    protected void onCreate() {
        this.fechaAnulacion = LocalDateTime.now();
    }
}