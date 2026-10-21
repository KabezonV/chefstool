package com.chefstool.backend.domain;

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

/** Compra confirmada a partir de un documento (o introducida a mano). */
@Entity
@Table(name = "compras")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id")
    private DocumentoCompra documento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id")
    private Evento evento;

    @Column(name = "numero_documento")
    private String numeroDocumento;

    private LocalDate fecha;

    @Column(nullable = false)
    private String moneda = "EUR";

    @Column(name = "importe_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeNeto = BigDecimal.ZERO;

    @Column(name = "importe_iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeIva = BigDecimal.ZERO;

    @Column(name = "importe_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal = BigDecimal.ZERO;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
