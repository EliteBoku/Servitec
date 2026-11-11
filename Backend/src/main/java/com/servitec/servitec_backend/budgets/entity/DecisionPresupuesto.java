package com.servitec.servitec_backend.budgets.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.shared.enums.TipoDecisionPresupuesto;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "decision_presupuesto")
public class DecisionPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_decision")
    private Integer idDecision;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presupuesto", nullable = false, unique = true)
    private Presupuesto presupuesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente_decisor", nullable = false)
    private Usuario clienteDecisor;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private TipoDecisionPresupuesto decision;

    @Column(name = "fecha_hora_decision", nullable = false)
    private LocalDateTime fechaHoraDecision;

    public DecisionPresupuesto() {
    }

    public Integer getIdDecision() {
        return idDecision;
    }

    public void setIdDecision(Integer idDecision) {
        this.idDecision = idDecision;
    }

    public Presupuesto getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(Presupuesto presupuesto) {
        this.presupuesto = presupuesto;
    }

    public Usuario getClienteDecisor() {
        return clienteDecisor;
    }

    public void setClienteDecisor(Usuario clienteDecisor) {
        this.clienteDecisor = clienteDecisor;
    }

    public TipoDecisionPresupuesto getDecision() {
        return decision;
    }

    public void setDecision(TipoDecisionPresupuesto decision) {
        this.decision = decision;
    }

    public LocalDateTime getFechaHoraDecision() {
        return fechaHoraDecision;
    }

    public void setFechaHoraDecision(LocalDateTime fechaHoraDecision) {
        this.fechaHoraDecision = fechaHoraDecision;
    }
}