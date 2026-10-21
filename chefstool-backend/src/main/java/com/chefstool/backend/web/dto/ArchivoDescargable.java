package com.chefstool.backend.web.dto;

import org.springframework.core.io.Resource;

public record ArchivoDescargable(Resource recurso, String tipoMime, String nombre) {}
