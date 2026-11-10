package com.servitec.servitec_backend.unit.inventory;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.inventory.dto.MovimientoInventarioResponse;
import com.servitec.servitec_backend.inventory.dto.RegistrarMovimientoManualRequest;
import com.servitec.servitec_backend.inventory.entity.MovimientoInventario;
import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.inventory.repository.MovimientoInventarioRepository;
import com.servitec.servitec_backend.inventory.repository.RepuestoRepository;
import com.servitec.servitec_backend.inventory.repository.ReservaInventarioRepository;
import com.servitec.servitec_backend.inventory.service.InventoryMovementService;
import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import com.servitec.servitec_backend.shared.exception.BusinessRuleException;
import com.servitec.servitec_backend.shared.exception.InsufficientStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryMovementServiceTest {

    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private ReservaInventarioRepository reservaInventarioRepository;

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private InventoryMovementService inventoryMovementService;

    private Usuario admin;
    private Repuesto repuesto;

    @BeforeEach
    void setUp() {

        // Simulamos el usuario porque su constructor tiene acceso protected.
        admin = mock(Usuario.class);

        repuesto = new Repuesto();
        repuesto.setIdRepuesto(10);
        repuesto.setCodigoRepuesto("REF-CAP-035");
        repuesto.setNombre("Capacitor 35 uF");
        repuesto.setTipoItem(TipoItemCatalogo.REPUESTO);
        repuesto.setUnidadMedida("Unidad");
        repuesto.setExistenciasActuales(new BigDecimal("10.00"));
        repuesto.setActivo(true);
    }

    @Test
    @DisplayName("RN-15 / REQ-28: Debe bloquear una SALIDA manual que intente tocar unidades comprometidas en reservas activas")
    void debeBloquearSalidaManualQueSupereDisponiblePorReservasActivas() {

        // Existencias físicas = 10, Reservas activas = 7 -> Disponible = 3
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        when(repuestoRepository.findByIdWithLock(10))
                .thenReturn(Optional.of(repuesto));

        when(reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(10))
                .thenReturn(new BigDecimal("7.00"));

        // Intenta retirar 4 unidades (hay 10 físicas, pero solo 3 disponibles)
        RegistrarMovimientoManualRequest request = new RegistrarMovimientoManualRequest(
                10,
                TipoMovimientoInventario.SALIDA,
                new BigDecimal("4.00"),
                "Retiro manual"
        );

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryMovementService.registrarMovimientoManual(request, 1)
        );

        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("RN-15 / REQ-28: Debe bloquear un AJUSTE físico cuyo conteo sea inferior a las reservas activas")
    void debeBloquearAjusteInferiorAReservasActivas() {

        // Existencias físicas = 10, Reservas activas = 6
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        when(repuestoRepository.findByIdWithLock(10))
                .thenReturn(Optional.of(repuesto));

        when(reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(10))
                .thenReturn(new BigDecimal("6.00"));

        // Conteo físico reporta 5 unidades (inferior a las 6 reservadas)
        RegistrarMovimientoManualRequest request = new RegistrarMovimientoManualRequest(
                10,
                TipoMovimientoInventario.AJUSTE,
                new BigDecimal("5.00"),
                "Conteo de auditoría"
        );

        assertThrows(
                InsufficientStockException.class,
                () -> inventoryMovementService.registrarMovimientoManual(request, 1)
        );

        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CV-19: Debe rechazar un AJUSTE cuando el conteo físico coincide exactamente con las existencias actuales")
    void debeRechazarAjusteSinDiferencia() {

        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        when(repuestoRepository.findByIdWithLock(10))
                .thenReturn(Optional.of(repuesto));

        when(reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(10))
                .thenReturn(BigDecimal.ZERO);

        RegistrarMovimientoManualRequest request = new RegistrarMovimientoManualRequest(
                10,
                TipoMovimientoInventario.AJUSTE,
                new BigDecimal("10.00"),
                "Conteo sin variación"
        );

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> inventoryMovementService.registrarMovimientoManual(request, 1)
        );

        assertEquals("SIN_DIFERENCIA_AJUSTE", ex.getCodigoError());

        verify(movimientoInventarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CV-19: Debe permitir un AJUSTE negativo siempre que el conteo físico sea >= reservas activas")
    void debePermitirAjusteCuandoConteoEsIgualOSuperiorAReservasActivas() {

        // Configuramos los datos que necesita mapearAResponse() del usuario.
        when(admin.getIdUsuario()).thenReturn(1);
        when(admin.getNombreCompleto()).thenReturn("Admin Servitec");

        // Existencias físicas = 10, Reservas activas = 6
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(admin));

        when(repuestoRepository.findByIdWithLock(10))
                .thenReturn(Optional.of(repuesto));

        when(reservaInventarioRepository.sumCantidadReservadaActivaByRepuesto(10))
                .thenReturn(new BigDecimal("6.00"));

        when(movimientoInventarioRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(inv -> {
                    MovimientoInventario m = inv.getArgument(0);
                    m.setIdMovimiento(100);
                    return m;
                });

        // Conteo físico = 6 (se descuentan 4 unidades por merma).
        RegistrarMovimientoManualRequest request = new RegistrarMovimientoManualRequest(
                10,
                TipoMovimientoInventario.AJUSTE,
                new BigDecimal("6.00"),
                "Baja de 4 unidades por merma"
        );

        MovimientoInventarioResponse response =
                inventoryMovementService.registrarMovimientoManual(request, 1);

        assertEquals(new BigDecimal("10.00"), response.existenciasAnteriores());
        assertEquals(new BigDecimal("6.00"), response.existenciasResultantes());
        assertEquals(new BigDecimal("4.00"), response.cantidadMovimiento());
        assertEquals(new BigDecimal("0.00"), response.existenciasDisponiblesActuales());
    }
}