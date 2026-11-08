package com.servitec.servitec_backend.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "sesion_usuario",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sesion_usuario_refresh_hash",
                        columnNames = "refresh_token_hash"
                )
        },
        indexes = {
                @Index(
                        name = "idx_sesion_usuario_usuario",
                        columnList = "id_usuario"
                )
        }
)
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Integer idSesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "refresh_token_hash", nullable = false, length = 128)
    private String refreshTokenHash;

    @Column(name = "fecha_hora_creacion", nullable = false)
    private LocalDateTime fechaHoraCreacion;

    @Column(name = "fecha_hora_ultima_actividad", nullable = false)
    private LocalDateTime fechaHoraUltimaActividad;

    @Column(name = "fecha_hora_expiracion", nullable = false)
    private LocalDateTime fechaHoraExpiracion;

    @Column(name = "fecha_hora_revocacion")
    private LocalDateTime fechaHoraRevocacion;

    protected SesionUsuario() {
    }

    public SesionUsuario(
            Usuario usuario,
            String refreshTokenHash,
            LocalDateTime fechaHoraCreacion,
            LocalDateTime fechaHoraUltimaActividad,
            LocalDateTime fechaHoraExpiracion,
            LocalDateTime fechaHoraRevocacion
    ) {
        this.usuario = usuario;
        this.refreshTokenHash = refreshTokenHash;
        this.fechaHoraCreacion = fechaHoraCreacion;
        this.fechaHoraUltimaActividad = fechaHoraUltimaActividad;
        this.fechaHoraExpiracion = fechaHoraExpiracion;
        this.fechaHoraRevocacion = fechaHoraRevocacion;
    }

    public Integer getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(Integer idSesion) {
        this.idSesion = idSesion;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public LocalDateTime getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public void setFechaHoraCreacion(LocalDateTime fechaHoraCreacion) {
        this.fechaHoraCreacion = fechaHoraCreacion;
    }

    public LocalDateTime getFechaHoraUltimaActividad() {
        return fechaHoraUltimaActividad;
    }

    public void setFechaHoraUltimaActividad(
            LocalDateTime fechaHoraUltimaActividad
    ) {
        this.fechaHoraUltimaActividad = fechaHoraUltimaActividad;
    }

    public LocalDateTime getFechaHoraExpiracion() {
        return fechaHoraExpiracion;
    }

    public void setFechaHoraExpiracion(LocalDateTime fechaHoraExpiracion) {
        this.fechaHoraExpiracion = fechaHoraExpiracion;
    }

    public LocalDateTime getFechaHoraRevocacion() {
        return fechaHoraRevocacion;
    }

    public void setFechaHoraRevocacion(LocalDateTime fechaHoraRevocacion) {
        this.fechaHoraRevocacion = fechaHoraRevocacion;
    }
}
