package com.servitec.servitec_backend.budgets.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "presupuesto")
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_presupuesto")
    private Integer idPresupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenServicio ordenServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tecnico_creador", nullable = false)
    private Usuario tecnicoCreador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_autorizacion", unique = true)
    private AutorizacionNuevaCotizacion autorizacionNuevaCotizacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_presupuesto", nullable = false, length = 30)
    private TipoPresupuesto tipoPresupuesto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPresupuesto estado;

    @Column(name = "tarifa_hora_aprobada", precision = 15, scale = 2)
    private BigDecimal tarifaHoraAprobada;

    @Column(name = "horas_estimadas", precision = 10, scale = 2)
    private BigDecimal horasEstimadas;

    @Column(name = "valor_mano_obra_estimado", precision = 15, scale = 2)
    private BigDecimal valorManoObraEstimado;

    @Column(name = "subtotal_repuestos", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotalRepuestos = BigDecimal.ZERO;

    @Column(name = "total_presupuesto", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPresupuesto = BigDecimal.ZERO;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDateTime fechaEmision;

    public Presupuesto() {
    }

    public Integer getIdPresupuesto() {
        return idPresupuesto;
    }

    public void setIdPresupuesto(Integer idPresupuesto) {
        this.idPresupuesto = idPresupuesto;
    }

    public OrdenServicio getOrdenServicio() {
        return ordenServicio;
    }

    public void setOrdenServicio(OrdenServicio ordenServicio) {
        this.ordenServicio = ordenServicio;
    }

    public Usuario getTecnicoCreador() {
        return tecnicoCreador;
    }

    public void setTecnicoCreador(Usuario tecnicoCreador) {
        this.tecnicoCreador = tecnicoCreador;
    }

    public AutorizacionNuevaCotizacion getAutorizacionNuevaCotizacion() {
        return autorizacionNuevaCotizacion;
    }

    public void setAutorizacionNuevaCotizacion(AutorizacionNuevaCotizacion autorizacionNuevaCotizacion) {
        this.autorizacionNuevaCotizacion = autorizacionNuevaCotizacion;
    }

    public TipoPresupuesto getTipoPresupuesto() {
        return tipoPresupuesto;
    }

    public void setTipoPresupuesto(TipoPresupuesto tipoPresupuesto) {
        this.tipoPresupuesto = tipoPresupuesto;
    }

    public EstadoPresupuesto getEstado() {
        return estado;
    }

    public void setEstado(EstadoPresupuesto estado) {
        this.estado = estado;
    }

    public BigDecimal getTarifaHoraAprobada() {
        return tarifaHoraAprobada;
    }

    public void setTarifaHoraAprobada(BigDecimal tarifaHoraAprobada) {
        this.tarifaHoraAprobada = tarifaHoraAprobada;
    }

    public BigDecimal getHorasEstimadas() {
        return horasEstimadas;
    }

    public void setHorasEstimadas(BigDecimal horasEstimadas) {
        this.horasEstimadas = horasEstimadas;
    }

    public BigDecimal getValorManoObraEstimado() {
        return valorManoObraEstimado;
    }

    public void setValorManoObraEstimado(BigDecimal valorManoObraEstimado) {
        this.valorManoObraEstimado = valorManoObraEstimado;
    }

    public BigDecimal getSubtotalRepuestos() {
        return subtotalRepuestos;
    }

    public void setSubtotalRepuestos(BigDecimal subtotalRepuestos) {
        this.subtotalRepuestos = subtotalRepuestos;
    }

    public BigDecimal getTotalPresupuesto() {
        return totalPresupuesto;
    }

    public void setTotalPresupuesto(BigDecimal totalPresupuesto) {
        this.totalPresupuesto = totalPresupuesto;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }
}