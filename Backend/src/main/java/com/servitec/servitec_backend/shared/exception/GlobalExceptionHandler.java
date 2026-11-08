package com.servitec.servitec_backend.shared.exception;

import com.servitec.servitec_backend.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

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
                ex.getElementosSinDisponibilidad().isEmpty() ? null : ex.getElementosSinDisponibilidad()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler({InvalidStateTransitionException.class, BusinessRuleException.class})
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
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

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
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    @ExceptionHandler({ForbiddenAccessException.class, AccessDeniedException.class})
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
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
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
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

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
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}