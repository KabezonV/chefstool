package com.chefstool.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Ficha tecnica (escandallo) de una elaboracion. */
@Entity
@Table(name = "fichas")
@Getter
@Setter
@NoArgsConstructor
public class Ficha {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private Integer raciones = 1;

    @Column(name = "margen_objetivo", precision = 5, scale = 2)
    private BigDecimal margenObjetivo;

    @Column(name = "precio_venta", precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime deletedAt;
}
