package com.servitec.servitec_backend.notifications.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notifications")
public class NotificationProperties {

    private String from;

    private String frontendBaseUrl = "http://localhost:5173";

    private String activationPath = "/activar-cuenta";

    private String recoveryPath = "/restablecer-contrasena";

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public String getActivationPath() {
        return activationPath;
    }

    public void setActivationPath(String activationPath) {
        this.activationPath = activationPath;
    }

    public String getRecoveryPath() {
        return recoveryPath;
    }

    public void setRecoveryPath(String recoveryPath) {
        this.recoveryPath = recoveryPath;
    }
}