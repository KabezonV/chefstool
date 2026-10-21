package com.chefstool.backend.service;

import com.chefstool.backend.domain.Producto;
import com.chefstool.backend.repository.ProductoRepository;
import com.chefstool.backend.web.dto.ProductoRequest;
import com.chefstool.backend.web.dto.ProductoResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final OrganizacionService organizacionService;

    public ProductoService(
            ProductoRepository productoRepository, OrganizacionService organizacionService) {
        this.productoRepository = productoRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        UUID organizacionId = organizacionService.organizacionActual();
        return productoRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(organizacionId)
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();

        if (productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un producto llamado " + nombre);
        }

        Producto producto = new Producto();
        producto.setOrganizacionId(organizacionId);
        producto.setNombre(nombre);
        producto.setCategoria(request.categoria());
        if (request.unidadBase() != null && !request.unidadBase().isBlank()) {
            producto.setUnidadBase(request.unidadBase());
        }

        return ProductoResponse.from(productoRepository.save(producto));
    }
}
