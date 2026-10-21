package com.chefstool.backend.domain;

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
@Table(name = "miembros_organizacion")
@Getter
@Setter
@NoArgsConstructor
public class MiembroOrganizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organizacion_id", nullable = false)
    private UUID organizacionId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    private String email;
    private String nombre;

    @Column(nullable = false)
    private String rol = "miembro";

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
