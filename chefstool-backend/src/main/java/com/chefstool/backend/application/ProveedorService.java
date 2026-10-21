package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.ProveedorRequest;
import com.chefstool.backend.adapter.in.web.dto.ProveedorResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final OrganizacionService organizacionService;

    public ProveedorService(
            ProveedorRepository proveedorRepository, OrganizacionService organizacionService) {
        this.proveedorRepository = proveedorRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedorRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionService.organizacionActual())
                .stream()
                .map(ProveedorResponse::from)
                .toList();
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();
        comprobarNombreLibre(organizacionId, nombre);

        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(organizacionId);
        proveedor.setNombre(nombre);
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(UUID id, ProveedorRequest request) {
        Proveedor proveedor = buscar(id);
        String nombre = request.nombre().trim();

        if (!proveedor.getNombre().equalsIgnoreCase(nombre)) {
            comprobarNombreLibre(proveedor.getOrganizacionId(), nombre);
        }

        proveedor.setNombre(nombre);
        aplicar(proveedor, request);
        return ProveedorResponse.from(proveedorRepository.save(proveedor));
    }

    @Transactional
    public void eliminar(UUID id) {
        buscar(id).setDeletedAt(OffsetDateTime.now());
    }

    private void aplicar(Proveedor proveedor, ProveedorRequest request) {
        proveedor.setCif(request.cif());
        proveedor.setEmail(request.email());
        proveedor.setTelefono(request.telefono());
        proveedor.setNotas(request.notas());
    }

    private void comprobarNombreLibre(UUID organizacionId, String nombre) {
        if (proveedorRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un proveedor llamado " + nombre);
        }
    }

    private Proveedor buscar(UUID id) {
        return proveedorRepository
                .findById(id)
                .filter(proveedor -> proveedor.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado"));
    }
}
