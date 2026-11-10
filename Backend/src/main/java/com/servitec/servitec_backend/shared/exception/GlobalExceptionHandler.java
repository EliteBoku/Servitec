
package com.servitec.servitec_backend.shared.exception;

import com.servitec.servitec_backend.shared.api.ApiError;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Errores de validación de los DTO.
     * HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        ApiError apiError = new ApiError(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "DATOS_ENTRADA_INVALIDOS",
                "Completa o corrige los campos obligatorios marcados para continuar.",
                request.getRequestURI(),
                LocalDateTime.now(),
                errores,
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    /**
     * JSON mal formado o valores que no pueden convertirse
     * al tipo esperado, por ejemplo un RolNombre inexistente.
     * HTTP 400
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleInvalidJson(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                false,
                HttpStatus.BAD_REQUEST.value(),
                "JSON_INVALIDO",
                "El cuerpo de la solicitud contiene datos inválidos o valores no permitidos.",
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiError);
    }

    /**
     * Conflictos por integridad de datos en la base de datos.
     * Incluye posibles duplicados detectados directamente por MySQL.
     * HTTP 409
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn(
                "Conflicto de integridad de datos en {} {}",
                request.getMethod(),
                request.getRequestURI()
        );

        ApiError apiError = new ApiError(
                false,
                HttpStatus.CONFLICT.value(),
                "CONFLICTO_DATOS",
                "La operación entra en conflicto con un registro existente.",
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    /**
     * Disponibilidad insuficiente de inventario.
     * HTTP 409
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiError> handleInsufficientStock(
            InsufficientStockException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                false,
                HttpStatus.CONFLICT.value(),
                ex.getCodigoError(),
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                ex.getElementosSinDisponibilidad().isEmpty()
                        ? null
                        : ex.getElementosSinDisponibilidad()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    /**
     * Reglas de negocio y transiciones de estado inválidas.
     * HTTP 409
     */
    @ExceptionHandler({
            InvalidStateTransitionException.class,
            BusinessRuleException.class
    })
    public ResponseEntity<ApiError> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                false,
                HttpStatus.CONFLICT.value(),
                ex.getCodigoError(),
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiError);
    }

    /**
     * Recurso solicitado que no existe.
     * HTTP 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                false,
                HttpStatus.NOT_FOUND.value(),
                "RECURSO_NO_ENCONTRADO",
                ex.getMessage(),
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiError);
    }

    /**
     * Usuario autenticado sin permisos suficientes.
     * HTTP 403
     */
    @ExceptionHandler({
            ForbiddenAccessException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ApiError> handleForbidden(
            Exception ex,
            HttpServletRequest request) {

        String mensaje = (ex instanceof ForbiddenAccessException)
                ? ex.getMessage()
                : "No tienes permisos suficientes para realizar esta operación.";

        ApiError apiError = new ApiError(
                false,
                HttpStatus.FORBIDDEN.value(),
                "ACCESO_DENEGADO",
                mensaje,
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(apiError);
    }

    /**
     * Errores de autenticación producidos dentro del contexto MVC.
     * HTTP 401
     */
    @ExceptionHandler({
            BadCredentialsException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request) {

        ApiError apiError = new ApiError(
                false,
                HttpStatus.UNAUTHORIZED.value(),
                "NO_AUTENTICADO",
                "Credenciales inválidas. Por favor, verifique sus datos.",
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiError);
    }

    /**
     * Excepciones HTTP controladas desde los servicios.
     * Conserva el código de estado original.
     *
     * Ejemplos:
     * 400: rol no permitido.
     * 409: correo electrónico duplicado.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        String codigoError = switch (status.value()) {
            case 400 -> "SOLICITUD_INVALIDA";
            case 401 -> "NO_AUTENTICADO";
            case 403 -> "ACCESO_DENEGADO";
            case 404 -> "RECURSO_NO_ENCONTRADO";
            case 409 -> "CONFLICTO";
            default -> "ERROR_SOLICITUD";
        };

        String mensaje = ex.getReason() != null
                ? ex.getReason()
                : "No fue posible procesar la solicitud.";

        log.warn(
                "Solicitud rechazada: {} {} - HTTP {} - {}",
                request.getMethod(),
                request.getRequestURI(),
                status.value(),
                mensaje
        );

        ApiError apiError = new ApiError(
                false,
                status.value(),
                codigoError,
                mensaje,
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(status)
                .body(apiError);
    }

    /**
     * Errores inesperados no contemplados por los manejadores anteriores.
     * HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        log.error(
                "Error inesperado procesando {} {}",
                request.getMethod(),
                request.getRequestURI(),
                ex
        );

        ApiError apiError = new ApiError(
                false,
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "ERROR_INTERNO_SERVIDOR",
                "Ocurrió un error interno inesperado al procesar la solicitud.",
                request.getRequestURI(),
                LocalDateTime.now(),
                null,
                null
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiError);
    }
}
