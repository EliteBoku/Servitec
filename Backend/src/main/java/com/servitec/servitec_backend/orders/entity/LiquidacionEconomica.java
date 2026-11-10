package com.servitec.servitec_backend.orders.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidacion_economica")
public class LiquidacionEconomica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liquidacion")
    private Integer idLiquidacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cancelacion", nullable = false, unique = true)
    private CancelacionOrden cancelacionOrden;

    @Column(name = "segundos_efectivos", nullable = false)
    private Long segundosEfectivos;

    @Column(name = "tarifa_hora_aplicada", nullable = false, precision = 15, scale = 2)
    private BigDecimal tarifaHoraAplicada;

    @Column(name = "valor_mano_obra", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorManoObra;

    @Column(name = "valor_repuestos_insumos", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorRepuestosInsumos;

    @Column(name = "total_calculado", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCalculado;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    public LiquidacionEconomica() {
    }

    public Integer getIdLiquidacion() {
        return idLiquidacion;
    }

    public void setIdLiquidacion(Integer idLiquidacion) {
        this.idLiquidacion = idLiquidacion;
    }

    public CancelacionOrden getCancelacionOrden() {
        return cancelacionOrden;
    }

    public void setCancelacionOrden(CancelacionOrden cancelacionOrden) {
        this.cancelacionOrden = cancelacionOrden;
    }

    public Long getSegundosEfectivos() {
        return segundosEfectivos;
    }

    public void setSegundosEfectivos(Long segundosEfectivos) {
        this.segundosEfectivos = segundosEfectivos;
    }

    public BigDecimal getTarifaHoraAplicada() {
        return tarifaHoraAplicada;
    }

    public void setTarifaHoraAplicada(BigDecimal tarifaHoraAplicada) {
        this.tarifaHoraAplicada = tarifaHoraAplicada;
    }

    public BigDecimal getValorManoObra() {
        return valorManoObra;
    }

    public void setValorManoObra(BigDecimal valorManoObra) {
        this.valorManoObra = valorManoObra;
    }

    public BigDecimal getValorRepuestosInsumos() {
        return valorRepuestosInsumos;
    }

    public void setValorRepuestosInsumos(BigDecimal valorRepuestosInsumos) {
        this.valorRepuestosInsumos = valorRepuestosInsumos;
    }

    public BigDecimal getTotalCalculado() {
        return totalCalculado;
    }

    public void setTotalCalculado(BigDecimal totalCalculado) {
        this.totalCalculado = totalCalculado;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(LocalDateTime fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }
}