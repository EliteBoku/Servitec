package com.servitec.servitec_backend.budgets.entity;

import com.servitec.servitec_backend.inventory.entity.Repuesto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_presupuesto")
public class DetallePresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false)
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_repuesto", nullable = false)
    private Repuesto repuesto;

    @Column(name = "cantidad_autorizada", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadAutorizada;

    @Column(name = "precio_unitario_conservado", nullable = false, precision = 15, scale = 2)
    private BigDecimal precioUnitarioConservado;

    @Column(name = "cantidad_utilizada", nullable = false, precision = 12, scale = 2)
    private BigDecimal cantidadUtilizada = BigDecimal.ZERO;

    @Column(name = "justificacion", columnDefinition = "TEXT")
    private String justificacion;

    public DetallePresupuesto() {
    }

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Presupuesto getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(Presupuesto presupuesto) {
        this.presupuesto = presupuesto;
    }

    public Repuesto getRepuesto() {
        return repuesto;
    }

    public void setRepuesto(Repuesto repuesto) {
        this.repuesto = repuesto;
    }

    public BigDecimal getCantidadAutorizada() {
        return cantidadAutorizada;
    }

    public void setCantidadAutorizada(BigDecimal cantidadAutorizada) {
        this.cantidadAutorizada = cantidadAutorizada;
    }

    public BigDecimal getPrecioUnitarioConservado() {
        return precioUnitarioConservado;
    }

    public void setPrecioUnitarioConservado(BigDecimal precioUnitarioConservado) {
        this.precioUnitarioConservado = precioUnitarioConservado;
    }

    public BigDecimal getCantidadUtilizada() {
        return cantidadUtilizada;
    }

    public void setCantidadUtilizada(BigDecimal cantidadUtilizada) {
        this.cantidadUtilizada = cantidadUtilizada;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public void setJustificacion(String justificacion) {
        this.justificacion = justificacion;
    }
}