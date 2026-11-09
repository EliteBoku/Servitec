package com.servitec.servitec_backend.security.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.security.jwt")
@Validated
public class JwtProperties {

    @NotBlank
    private String secret;

    @NotBlank
    private String issuer = "servitec-api";

    @Min(1)
    private long accessTokenExpirationMinutes = 15;

    @Min(1)
    private long inactivityTimeoutMinutes = 60;

    private long refreshTokenExpirationDays = 7;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public long getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    public long getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public void setAccessTokenExpirationMinutes(
            long accessTokenExpirationMinutes
    ) {
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public long getInactivityTimeoutMinutes() {
        return inactivityTimeoutMinutes;
    }

    public void setInactivityTimeoutMinutes(
            long inactivityTimeoutMinutes
    ) {
        this.inactivityTimeoutMinutes = inactivityTimeoutMinutes;
    }

    public void setRefreshTokenExpirationDays(
            long refreshTokenExpirationDays
    ) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }
}
