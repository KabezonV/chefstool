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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Albaran o factura subido, pendiente de revision o ya confirmado. */
@Entity
@Table(name = "documentos_compra")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoCompra {

    public static final String PENDIENTE_REVISION = "pendiente_revision";
    public static final String CONFIRMADO = "confirmado";
    public static final String DESCARTADO = "descartado";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(name = "nombre_original", nullable = false)
    private String nombreOriginal;

    @Column(name = "ruta_almacenamiento")
    private String rutaAlmacenamiento;

    @Column(name = "tipo_mime")
    private String tipoMime;

    @Column(nullable = false)
    private String estado = PENDIENTE_REVISION;

    @Column(nullable = false)
    private String origen = "subida";

    @Column(name = "confianza_extraccion", precision = 4, scale = 2)
    private BigDecimal confianzaExtraccion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extraccion_json")
    private String extraccionJson;

    @Column(name = "error_extraccion")
    private String errorExtraccion;

    @Column(name = "subido_por")
    private UUID subidoPor;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
