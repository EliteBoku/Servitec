package com.servitec.servitec_backend.orders.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimiento_datos")
public class ConsentimientoDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consentimiento")
    private Integer idConsentimiento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Usuario cliente;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden_primera_solicitud", nullable = false, unique = true)
    private OrdenServicio ordenPrimeraSolicitud;

    @Column(name = "autorizado", nullable = false)
    private Boolean autorizado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "version_politica", nullable = false, length = 30)
    private String versionPolitica;

    public ConsentimientoDatos() {
    }

    public Integer getIdConsentimiento() {
        return idConsentimiento;
    }

    public void setIdConsentimiento(Integer idConsentimiento) {
        this.idConsentimiento = idConsentimiento;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public OrdenServicio getOrdenPrimeraSolicitud() {
        return ordenPrimeraSolicitud;
    }

    public void setOrdenPrimeraSolicitud(OrdenServicio ordenPrimeraSolicitud) {
        this.ordenPrimeraSolicitud = ordenPrimeraSolicitud;
    }

    public Boolean getAutorizado() {
        return autorizado;
    }

    public void setAutorizado(Boolean autorizado) {
        this.autorizado = autorizado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getVersionPolitica() {
        return versionPolitica;
    }

    public void setVersionPolitica(String versionPolitica) {
        this.versionPolitica = versionPolitica;
    }
}