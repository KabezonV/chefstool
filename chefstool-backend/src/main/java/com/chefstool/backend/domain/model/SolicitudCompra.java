package com.chefstool.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "solicitudes_compra",
        indexes = {
            @Index(
                    name = "solicitudes_compra_organizacion_local_fecha_idx",
                    columnList = "organizacion_id, local_id, created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class SolicitudCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(name = "local_id")
    private UUID localId;

    @Column(name = "solicitante_id")
    private UUID solicitanteId;

    @Column(nullable = false, length = 160)
    private String concepto;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 200)
    private String proveedor;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoSolicitudCompra estado = EstadoSolicitudCompra.PENDIENTE;

    @Column(name = "revisada_por")
    private UUID revisadaPor;

    @Column(name = "revisada_at")
    private OffsetDateTime revisadaAt;

    @Column(name = "motivo_rechazo", length = 1000)
    private String motivoRechazo;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
