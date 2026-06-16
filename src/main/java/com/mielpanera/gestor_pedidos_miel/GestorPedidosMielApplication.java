package com.mielpanera.gestor_pedidos_miel;

import com.mielpanera.gestor_pedidos_miel.processor.OrderProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GestorPedidosMielApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorPedidosMielApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(OrderProcessor orderProcessor) {
		return (args) -> {
			orderProcessor.procesarPedidos();
		};
	}
}
