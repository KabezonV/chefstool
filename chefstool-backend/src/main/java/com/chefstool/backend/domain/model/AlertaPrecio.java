package com.chefstool.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Aviso de subida de precio detectado al confirmar una compra. */
@Entity
@Table(name = "alertas_precio")
@Getter
@Setter
@NoArgsConstructor
public class AlertaPrecio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private String severidad = "aviso";

    @Column(name = "porcentaje_cambio", precision = 6, scale = 2)
    private BigDecimal porcentajeCambio;

    @Column(name = "precio_anterior", precision = 12, scale = 4)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", precision = 12, scale = 4)
    private BigDecimal precioNuevo;

    @Column(nullable = false)
    private boolean reconocida = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
