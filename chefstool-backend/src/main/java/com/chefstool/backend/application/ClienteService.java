package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.Cliente;
import com.chefstool.backend.domain.port.out.ClienteRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.ClienteRequest;
import com.chefstool.backend.adapter.in.web.dto.ClienteResponse;
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
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OrganizacionService organizacionService;

    public ClienteService(
            ClienteRepository clienteRepository, OrganizacionService organizacionService) {
        this.clienteRepository = clienteRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public Pagina<ClienteResponse> listar(
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
                                "nombre", "email"));

        Page<Cliente> resultado =
                busqueda == null || busqueda.isBlank()
                        ? clienteRepository.findByOrganizacionIdAndDeletedAtIsNull(
                                organizacionId, pageable)
                        : clienteRepository
                                .findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                                        organizacionId, busqueda.trim(), pageable);

        return Pagina.desde(resultado).map(ClienteResponse::from);
    }

    @Transactional
    public ClienteResponse crear(ClienteRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();
        comprobarNombreLibre(organizacionId, nombre);

        Cliente cliente = new Cliente();
        cliente.setOrganizacionId(organizacionId);
        cliente.setNombre(nombre);
        aplicar(cliente, request);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizar(UUID id, ClienteRequest request) {
        Cliente cliente = buscar(id);
        String nombre = request.nombre().trim();

        if (!cliente.getNombre().equalsIgnoreCase(nombre)) {
            comprobarNombreLibre(cliente.getOrganizacionId(), nombre);
        }

        cliente.setNombre(nombre);
        aplicar(cliente, request);
        return ClienteResponse.from(clienteRepository.save(cliente));
    }

    @Transactional
    public void eliminar(UUID id) {
        buscar(id).setDeletedAt(OffsetDateTime.now());
    }

    private void aplicar(Cliente cliente, ClienteRequest request) {
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        cliente.setNotas(request.notas());
    }

    private void comprobarNombreLibre(UUID organizacionId, String nombre) {
        if (clienteRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un cliente llamado " + nombre);
        }
    }

    private Cliente buscar(UUID id) {
        return clienteRepository
                .findById(id)
                .filter(cliente -> cliente.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }
}
