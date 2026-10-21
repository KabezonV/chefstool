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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Catalogo de precios de un producto en un proveedor concreto. */
@Entity
@Table(name = "proveedor_productos")
@Getter
@Setter
@NoArgsConstructor
public class ProveedorProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre_en_proveedor")
    private String nombreEnProveedor;

    private String unidad;

    @Column(name = "precio_ultimo", precision = 12, scale = 4)
    private BigDecimal precioUltimo;

    @Column(name = "fecha_ultima_compra")
    private LocalDate fechaUltimaCompra;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
