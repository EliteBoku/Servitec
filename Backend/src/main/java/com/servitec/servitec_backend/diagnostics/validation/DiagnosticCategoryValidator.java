package com.servitec.servitec_backend.diagnostics.validation;

import com.servitec.servitec_backend.diagnostics.dto.ParametroDiagnosticoRequest;
import com.servitec.servitec_backend.diagnostics.dto.PlantillaParametroCategoriaResponse;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DiagnosticCategoryValidator {

    private static final String MENSAJE_FALTAN_OBLIGATORIOS =
            "Faltan parámetros obligatorios para esta categoría de equipo";

    private static final Map<String, List<PlantillaParametroCategoriaResponse>> PLANTILLAS_POR_CATEGORIA = Map.of(
            "REFRIGERACION", List.of(
                    new PlantillaParametroCategoriaResponse("PRESION_SUCCION", "Presión de succión", "psi", true, true),
                    new PlantillaParametroCategoriaResponse("PRESION_DESCARGA", "Presión de descarga", "psi", false, true),
                    new PlantillaParametroCategoriaResponse("VOLTAJE_ALIMENTACION", "Voltaje de alimentación", "V", true, true),
                    new PlantillaParametroCategoriaResponse("ESTADO_FISICO_EQUIPO", "Estado físico del equipo", "N/A", true, false)
            ),
            "CLIMATIZACION", List.of(
                    new PlantillaParametroCategoriaResponse("TEMPERATURA_ENTRADA", "Temperatura de entrada", "°C", true, true),
                    new PlantillaParametroCategoriaResponse("TEMPERATURA_SALIDA", "Temperatura de salida", "°C", true, true),
                    new PlantillaParametroCategoriaResponse("VOLTAJE_ALIMENTACION", "Voltaje de alimentación", "V", false, true),
                    new PlantillaParametroCategoriaResponse("CONDICION_FLUJO_AIRE", "Condición del flujo de aire", "N/A", false, false)
            ),
            "LINEA_BLANCA", List.of(
                    new PlantillaParametroCategoriaResponse("VOLTAJE_ALIMENTACION", "Voltaje de alimentación", "V", true, true),
                    new PlantillaParametroCategoriaResponse("RESPUESTA_CICLO_OPERACION", "Respuesta del ciclo de operación", "N/A", true, false),
                    new PlantillaParametroCategoriaResponse("PRESENCIA_FUGAS", "Presencia de fugas", "N/A", false, false),
                    new PlantillaParametroCategoriaResponse("CONDICION_FISICA_GENERAL", "Condición física general", "N/A", false, false)
            ),
            "ELECTRICIDAD", List.of(
                    new PlantillaParametroCategoriaResponse("VOLTAJE_MEDIDO", "Voltaje medido", "V", true, true),
                    new PlantillaParametroCategoriaResponse("CONTINUIDAD_CIRCUITO", "Continuidad del circuito", "N/A", true, false),
                    new PlantillaParametroCategoriaResponse("CONDICION_CABLEADO", "Condición del cableado", "N/A", true, false),
                    new PlantillaParametroCategoriaResponse("CORRIENTE_MEDIDA", "Corriente medida", "A", false, true)
            )
    );

    public List<PlantillaParametroCategoriaResponse> obtenerPlantillaPorCategoria(String nombreCategoria) {
        String claveCategoria = normalizarClaveCategoria(nombreCategoria);
        List<PlantillaParametroCategoriaResponse> plantilla = PLANTILLAS_POR_CATEGORIA.get(claveCategoria);
        if (plantilla == null) {
            throw new BusinessRuleException(
                    "CATEGORIA_DIAGNOSTICO_NO_SOPORTADA",
                    "No existe una plantilla de diagnóstico configurada para la categoría: " + nombreCategoria
            );
        }
        return plantilla;
    }

    public List<ParametroNormalizado> validarYNormalizarParametros(
            String nombreCategoria,
            List<ParametroDiagnosticoRequest> parametrosEnviados) {

        if (parametrosEnviados == null || parametrosEnviados.isEmpty()) {
            throw new BusinessRuleException("PARAMETROS_DIAGNOSTICO_FALTANTES", MENSAJE_FALTAN_OBLIGATORIOS);
        }

        List<PlantillaParametroCategoriaResponse> plantilla = obtenerPlantillaPorCategoria(nombreCategoria);

        Map<String, ParametroDiagnosticoRequest> mapaEnviados = new LinkedHashMap<>();
        for (ParametroDiagnosticoRequest req : parametrosEnviados) {
            if (req.nombreParametro() != null && !req.nombreParametro().isBlank()
                    && req.valor() != null && !req.valor().isBlank()) {
                mapaEnviados.put(normalizarTexto(req.nombreParametro()), req);
            }
        }

        List<ParametroNormalizado> resultado = new ArrayList<>();

        for (PlantillaParametroCategoriaResponse def : plantilla) {
            ParametroDiagnosticoRequest encontrado = buscarParametro(mapaEnviados, def);

            if (encontrado == null || encontrado.valor() == null || encontrado.valor().isBlank()) {
                if (def.obligatorio()) {
                    throw new BusinessRuleException("PARAMETROS_DIAGNOSTICO_FALTANTES", MENSAJE_FALTAN_OBLIGATORIOS);
                }
                continue;
            }

            String valorLimpio = encontrado.valor().trim();
            if (def.numerico()) {
                validarValorNumerico(def, valorLimpio);
            }

            String unidadFinal = (encontrado.unidadMedida() != null && !encontrado.unidadMedida().isBlank())
                    ? encontrado.unidadMedida().trim()
                    : def.unidadMedidaPorDefecto();

            resultado.add(new ParametroNormalizado(def.etiqueta(), valorLimpio, unidadFinal));
        }

        return resultado;
    }

    private ParametroDiagnosticoRequest buscarParametro(
            Map<String, ParametroDiagnosticoRequest> mapaEnviados,
            PlantillaParametroCategoriaResponse def) {

        ParametroDiagnosticoRequest porCodigo = mapaEnviados.get(normalizarTexto(def.codigoParametro()));
        if (porCodigo != null) {
            return porCodigo;
        }
        return mapaEnviados.get(normalizarTexto(def.etiqueta()));
    }

    private void validarValorNumerico(PlantillaParametroCategoriaResponse def, String valor) {
        try {
            BigDecimal numero = new BigDecimal(valor.replace(",", "."));
            if (def.codigoParametro().startsWith("VOLTAJE") && numero.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException(
                        "PARAMETRO_DIAGNOSTICO_INVALIDO",
                        "El parámetro '" + def.etiqueta() + "' debe ser un valor numérico mayor que cero."
                );
            }
            if (def.codigoParametro().equals("CORRIENTE_MEDIDA") && numero.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessRuleException(
                        "PARAMETRO_DIAGNOSTICO_INVALIDO",
                        "El parámetro '" + def.etiqueta() + "' no puede ser negativo."
                );
            }
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException(
                    "PARAMETRO_DIAGNOSTICO_INVALIDO",
                    "El parámetro '" + def.etiqueta() + "' requiere un valor numérico válido."
            );
        }
    }

    private String normalizarClaveCategoria(String nombreCategoria) {
        String normalizado = normalizarTexto(nombreCategoria);
        if (normalizado.contains("REFRIGERACION")) {
            return "REFRIGERACION";
        }
        if (normalizado.contains("CLIMATIZACION")) {
            return "CLIMATIZACION";
        }
        if (normalizado.contains("LINEA_BLANCA") || normalizado.contains("LINEA BLANCA")) {
            return "LINEA_BLANCA";
        }
        if (normalizado.contains("ELECTRICIDAD")) {
            return "ELECTRICIDAD";
        }
        return normalizado;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toUpperCase(Locale.ROOT).replace(" ", "_");
    }

    public record ParametroNormalizado(
            String nombreParametro,
            String valor,
            String unidadMedida
    ) {
    }
}