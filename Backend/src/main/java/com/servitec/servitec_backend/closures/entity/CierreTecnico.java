package com.servitec.servitec_backend.closures.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
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
@Table(name = "cierre_tecnico")
public class CierreTecnico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cierre")
    private Integer idCierre;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false, unique = true)
    private OrdenServicio ordenServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tecnico", nullable = false)
    private Usuario tecnico;

    @Column(name = "pruebas_funcionamiento", nullable = false, columnDefinition = "TEXT")
    private String pruebasFuncionamiento;

    @Column(name = "recomendaciones", nullable = false, columnDefinition = "TEXT")
    private String recomendaciones;

    @Column(name = "garantia_alcance", nullable = false, columnDefinition = "TEXT")
    private String garantiaAlcance;

    @Column(name = "garantia_vigencia", nullable = false, length = 100)
    private String garantiaVigencia;

    @Column(name = "fecha_cierre", nullable = false)
    private LocalDateTime fechaCierre;

    public CierreTecnico() {
    }

    public Integer getIdCierre() {
        return idCierre;
    }

    public void setIdCierre(Integer idCierre) {
        this.idCierre = idCierre;
    }

    public OrdenServicio getOrdenServicio() {
        return ordenServicio;
    }

    public void setOrdenServicio(OrdenServicio ordenServicio) {
        this.ordenServicio = ordenServicio;
    }

    public Usuario getTecnico() {
        return tecnico;
    }

    public void setTecnico(Usuario tecnico) {
        this.tecnico = tecnico;
    }

    public String getPruebasFuncionamiento() {
        return pruebasFuncionamiento;
    }

    public void setPruebasFuncionamiento(String pruebasFuncionamiento) {
        this.pruebasFuncionamiento = pruebasFuncionamiento;
    }

    public String getRecomendaciones() {
        return recomendaciones;
    }

    public void setRecomendaciones(String recomendaciones) {
        this.recomendaciones = recomendaciones;
    }

    public String getGarantiaAlcance() {
        return garantiaAlcance;
    }

    public void setGarantiaAlcance(String garantiaAlcance) {
        this.garantiaAlcance = garantiaAlcance;
    }

    public String getGarantiaVigencia() {
        return garantiaVigencia;
    }

    public void setGarantiaVigencia(String garantiaVigencia) {
        this.garantiaVigencia = garantiaVigencia;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
}