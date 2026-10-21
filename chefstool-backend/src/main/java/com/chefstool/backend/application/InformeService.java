package com.chefstool.backend.application;

import com.chefstool.backend.application.dto.AlertaResumen;
import com.chefstool.backend.application.dto.DocumentoPendiente;
import com.chefstool.backend.application.dto.GastoAgrupado;
import com.chefstool.backend.application.dto.KpiPanel;
import com.chefstool.backend.application.dto.PanelInforme;
import com.chefstool.backend.application.dto.VariacionPrecio;
import com.chefstool.backend.domain.model.AlertaPrecio;
import com.chefstool.backend.domain.model.Compra;
import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.model.LineaCompra;
import com.chefstool.backend.domain.port.out.AlertaPrecioRepository;
import com.chefstool.backend.domain.port.out.CompraRepository;
import com.chefstool.backend.domain.port.out.DocumentoCompraRepository;
import com.chefstool.backend.domain.port.out.LineaCompraRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Agrega los datos de compras y precios para el panel y los informes. */
@Service
public class InformeService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final CompraRepository compraRepository;
    private final LineaCompraRepository lineaCompraRepository;
    private final DocumentoCompraRepository documentoRepository;
    private final AlertaPrecioRepository alertaRepository;
    private final OrganizacionService organizacionService;

    public InformeService(
            CompraRepository compraRepository,
            LineaCompraRepository lineaCompraRepository,
            DocumentoCompraRepository documentoRepository,
            AlertaPrecioRepository alertaRepository,
            OrganizacionService organizacionService) {
        this.compraRepository = compraRepository;
        this.lineaCompraRepository = lineaCompraRepository;
        this.documentoRepository = documentoRepository;
        this.alertaRepository = alertaRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public PanelInforme panel() {
        UUID organizacionId = organizacionService.organizacionActual();
        List<Compra> compras = compraRepository.buscar(organizacionId, null, null, null);

        YearMonth mesActual = YearMonth.now();
        YearMonth mesAnterior = mesActual.minusMonths(1);

        BigDecimal gastoMes = gastoDelMes(compras, mesActual);
        BigDecimal gastoMesAnterior = gastoDelMes(compras, mesAnterior);
        long comprasMes = compras.stream().filter(c -> enMes(c, mesActual)).count();

        List<GastoAgrupado> porProveedor = gastoPorProveedor(compras, mesActual);
        List<GastoAgrupado> porMes = gastoPorMes(compras, mesActual);

        List<LineaCompra> lineas = lineaCompraRepository.lineasDeOrganizacion(organizacionId);
        Map<UUID, BigDecimal> mejoresPrecios = mejoresPrecios(lineas);
        BigDecimal ahorro = BigDecimal.ZERO;
        int mejorables = 0;

        for (LineaCompra linea : lineas) {
            if (linea.getProducto() == null) {
                continue;
            }
            BigDecimal pagado = precio(linea);
            BigDecimal mejor = mejoresPrecios.get(linea.getProducto().getId());
            if (pagado == null || mejor == null || pagado.compareTo(mejor) <= 0) {
                continue;
            }
            ahorro = ahorro.add(pagado.subtract(mejor).multiply(linea.getCantidad()));
            mejorables++;
        }

        List<AlertaPrecio> pendientes =
                alertaRepository.findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(
                        organizacionId);
        List<DocumentoCompra> documentosPendientes =
                documentoRepository.findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(
                        organizacionId, DocumentoCompra.PENDIENTE_REVISION);

        KpiPanel kpis =
                new KpiPanel(
                        gastoMes,
                        gastoMesAnterior,
                        variacion(gastoMes, gastoMesAnterior),
                        comprasMes,
                        documentosPendientes.size(),
                        pendientes.size());

        BigDecimal gastoTotal =
                compras.stream()
                        .map(compra -> nz(compra.getImporteTotal()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);

        return new PanelInforme(
                kpis,
                ahorro.setScale(2, RoundingMode.HALF_UP),
                mejorables,
                compras.size(),
                gastoTotal,
                porProveedor,
                porMes,
                variaciones(organizacionId),
                pendientes.stream().limit(5).map(AlertaResumen::from).toList(),
                documentosPendientes.stream().limit(5).map(DocumentoPendiente::from).toList());
    }

    private List<GastoAgrupado> gastoPorProveedor(List<Compra> compras, YearMonth mes) {
        Map<String, BigDecimal> acumulado = new LinkedHashMap<>();
        for (Compra compra : compras) {
            if (!enMes(compra, mes)) {
                continue;
            }
            String etiqueta =
                    compra.getProveedor() != null
                            ? compra.getProveedor().getNombre()
                            : "Sin proveedor";
            acumulado.merge(etiqueta, nz(compra.getImporteTotal()), BigDecimal::add);
        }

        return acumulado.entrySet().stream()
                .map(entrada -> new GastoAgrupado(entrada.getKey(), entrada.getValue()))
                .sorted(Comparator.comparing(GastoAgrupado::importe).reversed())
                .limit(8)
                .toList();
    }

    private List<GastoAgrupado> gastoPorMes(List<Compra> compras, YearMonth mesActual) {
        List<GastoAgrupado> serie = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth mes = mesActual.minusMonths(i);
            serie.add(new GastoAgrupado(mes.toString(), gastoDelMes(compras, mes)));
        }
        return serie;
    }

    private List<VariacionPrecio> variaciones(UUID organizacionId) {
        return alertaRepository.todas(organizacionId).stream()
                .filter(alerta -> alerta.getPorcentajeCambio() != null)
                .sorted(
                        Comparator.comparing(AlertaPrecio::getPorcentajeCambio)
                                .reversed())
                .limit(5)
                .map(
                        alerta ->
                                new VariacionPrecio(
                                        alerta.getProducto() != null
                                                ? alerta.getProducto().getNombre()
                                                : "—",
                                        alerta.getProveedor() != null
                                                ? alerta.getProveedor().getNombre()
                                                : "—",
                                        alerta.getPorcentajeCambio(),
                                        alerta.getPrecioAnterior(),
                                        alerta.getPrecioNuevo()))
                .toList();
    }

    private Map<UUID, BigDecimal> mejoresPrecios(List<LineaCompra> lineas) {
        return lineas.stream()
                .filter(linea -> linea.getProducto() != null && precio(linea) != null)
                .collect(
                        Collectors.toMap(
                                linea -> linea.getProducto().getId(),
                                InformeService::precio,
                                (actual, nuevo) -> actual.compareTo(nuevo) <= 0 ? actual : nuevo));
    }

    private BigDecimal gastoDelMes(List<Compra> compras, YearMonth mes) {
        return compras.stream()
                .filter(compra -> enMes(compra, mes))
                .map(compra -> nz(compra.getImporteTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private boolean enMes(Compra compra, YearMonth mes) {
        return compra.getFecha() != null && YearMonth.from(compra.getFecha()).equals(mes);
    }

    private BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() <= 0) {
            return null;
        }
        return actual.subtract(anterior)
                .multiply(CIEN)
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal precio(LineaCompra linea) {
        return linea.getPrecioNormalizado() != null
                ? linea.getPrecioNormalizado()
                : linea.getPrecioUnitario();
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
