package com.chefstool.backend.adapter.in.web.dto;

import org.springframework.core.io.Resource;

public record ArchivoDescargable(Resource recurso, String tipoMime, String nombre) {}
