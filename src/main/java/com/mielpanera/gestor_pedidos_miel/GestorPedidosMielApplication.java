package com.mielpanera.gestor_pedidos_miel;

import com.mielpanera.gestor_pedidos_miel.dto.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.service.CorreosScraperService;
import com.mielpanera.gestor_pedidos_miel.service.CorreosService;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class GestorPedidosMielApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestorPedidosMielApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(WooCommerceService wooService, CorreosScraperService correosScraperService, TelegramService telegramService) {
		return (args) -> {

			String statusCorreos1 = correosScraperService.obtenerEstadoActual("PQ6PCB9800014370148993C");
			System.out.println("PQ6PCB9800014370148993C" + "; estado: " + statusCorreos1);
			if (statusCorreos1.contains("disposición")) {
				System.out.println("jsadlkfjsajdlñjfl");
			}


/*			System.out.println("--- INICIANDO TEST DE CONEXIÓN ---");
			// List<PedidoDTO> orders = wooService.obtenerPedidos("prepared-cocex");
			List<PedidoDTO> orders = wooService.obtenerPedidos("processing");
			System.out.println("--- FIN DE TEST ---");

			for (PedidoDTO order: orders) {
				*//*
				String statusReal = correosScraperService.obtenerEstadoActual(order.getTrackingNumber());
				System.out.println(order.getBilling().getNombreCompleto() + ", status: " + statusReal);
				 *//*
				if (order.getId() == 2973) {
					telegramService.notificarPedidoEntregado(order);
				}
			}


			System.out.println("--- PRUEBA CORREOS ---");
			String trackingCode;
			for (PedidoDTO order: orders) {
				trackingCode = order.getTrackingNumber();


				String statusCorreos = correosScraperService.obtenerEstadoActual(trackingCode);
				System.out.println(trackingCode + "; estado: " + statusCorreos);

				if (statusCorreos.toUpperCase().contains("ENTREGADO")) {
					System.out.println("-> ¡Pedido completado! Actualizando Woo...");
					// wooClient.updateOrderStatus(pedido.id(), "completed");
				}

				break;
			}*/



		};
	}

}
