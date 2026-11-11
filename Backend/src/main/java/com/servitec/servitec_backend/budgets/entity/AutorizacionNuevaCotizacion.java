package com.servitec.servitec_backend.budgets.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "autorizacion_nueva_cotizacion")
public class AutorizacionNuevaCotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autorizacion")
    private Integer idAutorizacion;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto_rechazado", nullable = false, unique = true)
    private Presupuesto presupuestoRechazado;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_evaluacion", nullable = false, unique = true)
    private EvaluacionTecnica evaluacionTecnica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_administrador", nullable = false)
    private Usuario administrador;

    @Column(name = "fecha_hora_autorizacion", nullable = false)
    private LocalDateTime fechaHoraAutorizacion;

    public AutorizacionNuevaCotizacion() {
    }

    public Integer getIdAutorizacion() {
        return idAutorizacion;
    }

    public void setIdAutorizacion(Integer idAutorizacion) {
        this.idAutorizacion = idAutorizacion;
    }

    public Presupuesto getPresupuestoRechazado() {
        return presupuestoRechazado;
    }

    public void setPresupuestoRechazado(Presupuesto presupuestoRechazado) {
        this.presupuestoRechazado = presupuestoRechazado;
    }

    public EvaluacionTecnica getEvaluacionTecnica() {
        return evaluacionTecnica;
    }

    public void setEvaluacionTecnica(EvaluacionTecnica evaluacionTecnica) {
        this.evaluacionTecnica = evaluacionTecnica;
    }

    public Usuario getAdministrador() {
        return administrador;
    }

    public void setAdministrador(Usuario administrador) {
        this.administrador = administrador;
    }

    public LocalDateTime getFechaHoraAutorizacion() {
        return fechaHoraAutorizacion;
    }

    public void setFechaHoraAutorizacion(LocalDateTime fechaHoraAutorizacion) {
        this.fechaHoraAutorizacion = fechaHoraAutorizacion;
    }
}
