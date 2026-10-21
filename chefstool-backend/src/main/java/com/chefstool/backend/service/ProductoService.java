package com.chefstool.backend.service;

import com.chefstool.backend.domain.Producto;
import com.chefstool.backend.repository.ProductoRepository;
import com.chefstool.backend.web.RecursoNoEncontradoException;
import com.chefstool.backend.web.dto.ProductoRequest;
import com.chefstool.backend.web.dto.ProductoResponse;
import java.time.OffsetDateTime;
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
        return productoRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionService.organizacionActual())
                .stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();
        comprobarNombreLibre(organizacionId, nombre);

        Producto producto = new Producto();
        producto.setOrganizacionId(organizacionId);
        producto.setNombre(nombre);
        aplicar(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = buscar(id);
        String nombre = request.nombre().trim();

        if (!producto.getNombre().equalsIgnoreCase(nombre)) {
            comprobarNombreLibre(producto.getOrganizacionId(), nombre);
        }

        producto.setNombre(nombre);
        aplicar(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(UUID id) {
        Producto producto = buscar(id);
        producto.setDeletedAt(OffsetDateTime.now());
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setCategoria(request.categoria());
        if (request.unidadBase() != null && !request.unidadBase().isBlank()) {
            producto.setUnidadBase(request.unidadBase());
        }
    }

    private void comprobarNombreLibre(UUID organizacionId, String nombre) {
        if (productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un producto llamado " + nombre);
        }
    }

    private Producto buscar(UUID id) {
        return productoRepository
                .findById(id)
                .filter(producto -> producto.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }
}
