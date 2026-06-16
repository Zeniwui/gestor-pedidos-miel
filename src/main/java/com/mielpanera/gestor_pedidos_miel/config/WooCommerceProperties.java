package com.mielpanera.gestor_pedidos_miel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "woocommerce")
public class WooCommerceProperties {
    private String storeUrl;
    private String consumerKey;
    private String consumerSecret;
}
