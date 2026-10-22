package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.ProveedorRequest;
import com.chefstool.backend.adapter.in.web.dto.ProveedorResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.chefstool.backend.application.dto.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public Pagina<ProveedorResponse> listar(
            int pagina, int tamano, String busqueda, String orden, String direccion) {
        UUID organizacionId = organizacionService.organizacionActual();
        Pageable pageable =
                Pagina.peticion(
                        pagina,
                        tamano,
                        Pagina.orden(
                                orden,
                                direccion,
                                Sort.by("nombre").ascending(),
                                "nombre", "cif", "email"));

        Page<Proveedor> resultado =
                busqueda == null || busqueda.isBlank()
                        ? proveedorRepository.findByOrganizacionIdAndDeletedAtIsNull(
                                organizacionId, pageable)
                        : proveedorRepository
                                .findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                                        organizacionId, busqueda.trim(), pageable);

        return Pagina.desde(resultado).map(ProveedorResponse::from);
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
