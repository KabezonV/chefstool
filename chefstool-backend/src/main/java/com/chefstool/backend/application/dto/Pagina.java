package com.chefstool.backend.application.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Envoltorio de paginacion comun a todos los listados. */
public record Pagina<T>(
        List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    /** Peticion de pagina saneada: pagina >= 0 y tamano entre 1 y 100. */
    public static Pageable peticion(int pagina, int tamano, Sort orden) {
        return PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamano, 1), 100), orden);
    }

    /**
     * Orden pedido por el cliente validado contra una lista blanca; si no es valido
     * se queda el orden por defecto del recurso.
     */
    public static Sort orden(String campo, String direccion, Sort porDefecto, String... permitidos) {
        if (campo == null || campo.isBlank()) {
            return porDefecto;
        }
        String limpio = campo.trim();
        if (java.util.Arrays.stream(permitidos).noneMatch(permitido -> permitido.equals(limpio))) {
            return porDefecto;
        }
        return "desc".equalsIgnoreCase(direccion)
                ? Sort.by(limpio).descending()
                : Sort.by(limpio).ascending();
    }

    public static <T> Pagina<T> desde(Page<T> pagina) {
        return new Pagina<>(
                pagina.getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }

    public <R> Pagina<R> map(Function<T, R> conversion) {
        return new Pagina<>(
                contenido.stream().map(conversion).toList(),
                pagina,
                tamano,
                totalElementos,
                totalPaginas);
    }
}
