package com.servitec.servitec_backend.inventory.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_repuesto", nullable = false)
    private Repuesto repuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private Usuario usuarioRegistro;

    @Column(name = "id_detalle_presupuesto")
    private Integer idDetallePresupuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private TipoMovimientoInventario tipoMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_movimiento", nullable = false, length = 30)
    private OrigenMovimientoInventario origenMovimiento;

    @Column(name = "cantidad_movimiento", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadMovimiento;

    @Column(name = "cantidad_contada", precision = 12, scale = 2)
    private BigDecimal cantidadContada;

    @Column(name = "existencias_anteriores", nullable = false, precision = 12, scale = 2)
    private BigDecimal existenciasAnteriores;

    @Column(name = "existencias_resultantes", nullable = false, precision = 12, scale = 2)
    private BigDecimal existenciasResultantes;

    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_hora_movimiento", nullable = false)
    private LocalDateTime fechaHoraMovimiento;

    public MovimientoInventario() {
    }

    public Integer getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(Integer idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public Repuesto getRepuesto() {
        return repuesto;
    }

    public void setRepuesto(Repuesto repuesto) {
        this.repuesto = repuesto;
    }

    public Usuario getUsuarioRegistro() {
        return usuarioRegistro;
    }

    public void setUsuarioRegistro(Usuario usuarioRegistro) {
        this.usuarioRegistro = usuarioRegistro;
    }

    public Integer getIdDetallePresupuesto() {
        return idDetallePresupuesto;
    }

    public void setIdDetallePresupuesto(Integer idDetallePresupuesto) {
        this.idDetallePresupuesto = idDetallePresupuesto;
    }

    public TipoMovimientoInventario getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimientoInventario tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public OrigenMovimientoInventario getOrigenMovimiento() {
        return origenMovimiento;
    }

    public void setOrigenMovimiento(OrigenMovimientoInventario origenMovimiento) {
        this.origenMovimiento = origenMovimiento;
    }

    public BigDecimal getCantidadMovimiento() {
        return cantidadMovimiento;
    }

    public void setCantidadMovimiento(BigDecimal cantidadMovimiento) {
        this.cantidadMovimiento = cantidadMovimiento;
    }

    public BigDecimal getCantidadContada() {
        return cantidadContada;
    }

    public void setCantidadContada(BigDecimal cantidadContada) {
        this.cantidadContada = cantidadContada;
    }

    public BigDecimal getExistenciasAnteriores() {
        return existenciasAnteriores;
    }

    public void setExistenciasAnteriores(BigDecimal existenciasAnteriores) {
        this.existenciasAnteriores = existenciasAnteriores;
    }

    public BigDecimal getExistenciasResultantes() {
        return existenciasResultantes;
    }

    public void setExistenciasResultantes(BigDecimal existenciasResultantes) {
        this.existenciasResultantes = existenciasResultantes;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public LocalDateTime getFechaHoraMovimiento() {
        return fechaHoraMovimiento;
    }

    public void setFechaHoraMovimiento(LocalDateTime fechaHoraMovimiento) {
        this.fechaHoraMovimiento = fechaHoraMovimiento;
    }
}