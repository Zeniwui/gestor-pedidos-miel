package com.mielpanera.gestor_pedidos_miel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "correos")
public class CorreosProperties {
    private Map<Integer, String> productLabels = new HashMap<>();
}
