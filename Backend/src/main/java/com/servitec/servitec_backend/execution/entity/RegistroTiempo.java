package com.servitec.servitec_backend.execution.entity;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.shared.enums.TipoFinTiempo;
import com.servitec.servitec_backend.shared.enums.TipoInicioTiempo;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "registro_tiempo")
public class RegistroTiempo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_tiempo")
    private Integer idRegistroTiempo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenServicio ordenServicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tecnico", nullable = false)
    private Usuario tecnico;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_inicio", nullable = false, length = 30)
    private TipoInicioTiempo tipoInicio;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_fin", length = 30)
    private TipoFinTiempo tipoFin;

    @Column(name = "fecha_hora_fin")
    private LocalDateTime fechaHoraFin;

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    public RegistroTiempo() {
    }

    public Integer getIdRegistroTiempo() {
        return idRegistroTiempo;
    }

    public void setIdRegistroTiempo(Integer idRegistroTiempo) {
        this.idRegistroTiempo = idRegistroTiempo;
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

    public TipoInicioTiempo getTipoInicio() {
        return tipoInicio;
    }

    public void setTipoInicio(TipoInicioTiempo tipoInicio) {
        this.tipoInicio = tipoInicio;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public TipoFinTiempo getTipoFin() {
        return tipoFin;
    }

    public void setTipoFin(TipoFinTiempo tipoFin) {
        this.tipoFin = tipoFin;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public Long getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Long duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }
}