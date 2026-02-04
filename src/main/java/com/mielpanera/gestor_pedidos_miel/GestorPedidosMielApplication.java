package com.mielpanera.gestor_pedidos_miel;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.service.CorreosScraperService;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

				CorreosInfo info = correosScraperService.obtenerEstadoActual(order.getTrackingNumber());

				long diasSinMoverse = ChronoUnit.DAYS.between(info.fechaEvento(), LocalDate.now());
				String estadoMayus = info.estado().toUpperCase();

				System.out.printf("📦 ID: %-5s | 👤 %-15s | 🚚 %-15s | 📅 %-10s (+%d días) | ℹ️ %s%n",
						order.getId(),
						order.getBilling().getNombreCompleto(),
						order.getTrackingNumber(),
						info.fechaEvento(), // Fecha del último evento
						diasSinMoverse,     // Días transcurridos
						info.estado()
				);

				// A) ¿Está entregado? -> Actualizar Woo
				if (estadoMayus.contains("ENTREGADO")) {
					wooService.actualizarEstadoPedido(order, "completed");

				}
				// B) ¿Es una devolución? -> Marcar devuelto
				else if (estadoMayus.contains("DEVOLUCIÓN") || estadoMayus.contains("DEVUELTO")) {
					wooService.actualizarEstadoPedido(order, "returned-cocex");

				}
				// C) ¿Está en oficina esperando al cliente? -> Avisar por Telegram
				else if (estadoMayus.toUpperCase().contains("SE ENCUENTRA EN LA OFICINA DE CORREOS")) {
					telegramService.notificarPedidoDisposicion(order);
				}
				// D) Está parado
				else if (estadoMayus.contains("PARADO")) {
					System.err.println("⚠️ PEDIDO PARADO: PENDIENTE DE INDICACIONES");
					telegramService.alertarPedidoEstacionado(order);
				}
			}

			System.out.println("------------------------ FINALIZADO -------------------------------");
		};
	}

}
