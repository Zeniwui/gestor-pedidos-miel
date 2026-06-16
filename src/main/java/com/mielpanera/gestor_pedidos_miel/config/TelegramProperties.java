package com.mielpanera.gestor_pedidos_miel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String token;
    private String tokenPreparados;
    private String tokenDisposicion;
    private String tokenNoEnviados;
    private String tokenAlertas;
    private String tokenEstacionados;
    private String chatId;
    private String telefonoOficial;
}
