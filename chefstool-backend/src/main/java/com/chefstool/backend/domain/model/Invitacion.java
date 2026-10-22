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
@Table(name = "invitaciones")
@Getter
@Setter
@NoArgsConstructor
public class Invitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String rol = "miembro";

    @Column(name = "invitado_por")
    private UUID invitadoPor;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "aceptada_at")
    private OffsetDateTime aceptadaAt;
}
