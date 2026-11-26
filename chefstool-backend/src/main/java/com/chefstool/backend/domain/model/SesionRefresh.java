package com.chefstool.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "sesiones_refresh")
@Getter
@Setter
@NoArgsConstructor
public class SesionRefresh {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(name = "local_id")
    private UUID localId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expira_at", nullable = false)
    private OffsetDateTime expiraAt;

    @Column(name = "revocado_at")
    private OffsetDateTime revocadoAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
