package com.servitec.servitec_backend.shared.util;
import java.time.Year;

public final class OrderCodeGenerator {

    private OrderCodeGenerator() {
    }

    public static String formatearCodigoOrden(int anio, int secuenciaId) {
        return String.format("OS-%d-%04d", anio, secuenciaId);
    }

    public static String generarCodigoTemporal() {
        return "TEMP-" + System.nanoTime();
    }

    public static int anioActual() {
        return Year.now().getValue();
    }
}