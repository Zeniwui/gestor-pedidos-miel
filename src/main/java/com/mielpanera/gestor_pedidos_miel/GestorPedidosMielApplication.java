package com.mielpanera.gestor_pedidos_miel;

import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.service.CorreosScraperService;
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


/*			List<PedidoDTO> ordersProcessing = wooService.obtenerPedidos("processing");
			for (PedidoDTO order: ordersProcessing) {
				if (order.getId() == 2988) {
					wooService.addProductsInObservations(order);
					break;
				}
			}*/


			System.out.println("--- INICIANDO TEST DE CONEXIÓN ---");
			List<PedidoDTO> ordersPreparedCocex = wooService.obtenerPedidos("prepared-cocex");
			// List<PedidoDTO> ordersInProgressCocex = wooService.obtenerPedidos("inprogress-cocex");
			System.out.println("--- FIN DE TEST ---");

			for (PedidoDTO order: ordersPreparedCocex) {

				String statusReal = correosScraperService.obtenerEstadoActual(order.getTrackingNumber());

				System.out.printf("📦 Order ID: %-10s | 👤 %-20s | 🚚 Track: %-15s | ℹ️ %s%n",
						order.getId(),
						order.getBilling().getNombreCompleto(),
						order.getTrackingNumber(),
						statusReal
				);

				if (statusReal.toUpperCase().contains("ENTREGADO")) {
					wooService.actualizarEstadoPedido(order, "completed");
				} else if (statusReal.toUpperCase().contains("DEVOLUCIÓN")) {
					wooService.actualizarEstadoPedido(order, "returned-cocex");
				} else if (statusReal.toUpperCase().contains("SE ENCUENTRA EN LA OFICINA DE CORREOS")) {
					telegramService.notificarPedidoDisposicion(order);
				} else if (statusReal.toUpperCase().contains("PARADO")) {
					System.err.println("PENDIENTE DE INDICACIONES");
				}
			}



/*			System.out.println("--- PRUEBA CORREOS ---");
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
