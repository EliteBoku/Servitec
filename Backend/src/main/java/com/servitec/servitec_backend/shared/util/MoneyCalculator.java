package com.servitec.servitec_backend.shared.util;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class MoneyCalculator {

    private static final BigDecimal SEGUNDOS_POR_HORA = BigDecimal.valueOf(3600);

    private MoneyCalculator() {
    }

    /**
     * REQ-12: Costo estimado de mano de obra = tarifaHora * horasEstimadas (redondeado al peso más cercano).
     */
    public static BigDecimal calcularManoObraEstimada(BigDecimal tarifaHora, BigDecimal horasEstimadas) {
        if (tarifaHora == null || horasEstimadas == null) {
            return BigDecimal.ZERO;
        }
        return tarifaHora.multiply(horasEstimadas).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * REQ-26 y RN-13: Valor final de mano de obra = tarifaHoraAprobada * (segundosEfectivosAcumulados / 3600).
     * Sin redondeos intermedios y con un único redondeo final al peso colombiano más cercano.
     */
    public static BigDecimal calcularManoObraEfectiva(BigDecimal tarifaHoraAprobada, long segundosEfectivosAcumulados) {
        if (tarifaHoraAprobada == null || segundosEfectivosAcumulados <= 0) {
            return BigDecimal.ZERO;
        }
        return tarifaHoraAprobada
                .multiply(BigDecimal.valueOf(segundosEfectivosAcumulados), MathContext.DECIMAL128)
                .divide(SEGUNDOS_POR_HORA, 0, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el importe de un ítem (precioUnitario * cantidad) redondeado al peso colombiano más cercano.
     */
    public static BigDecimal calcularImporteItem(BigDecimal precioUnitario, BigDecimal cantidad) {
        if (precioUnitario == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(cantidad).setScale(0, RoundingMode.HALF_UP);
    }
}