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


			//PONER TODOS LOS PEDIDOS CON LA MARCA DE "YA AVISADO"
/*			System.out.println("--- 🚀 INICIANDO MARCADO MASIVO DE PEDIDOS PREPARADOS ---");
			List<PedidoDTO> pedidosPreparados = wooService.obtenerPedidos("prepared-cocex");
			System.out.println("Se han encontrado " + pedidosPreparados.size() + " pedidos para actualizar.");
			String claveTelegram = "_aviso_telegram_preparado";
			for (PedidoDTO order : pedidosPreparados) {
				boolean yaTieneMetaDato = wooService.comprobarAccionMetaData(order, claveTelegram);
				if (!yaTieneMetaDato) {
					System.out.println("Añadiendo marca de 'ya avisado' al pedido #" + order.getId() + "...");
					wooService.actualizarMetaData(order, claveTelegram, "true");
					wooService.imprimirMetaDataVisual(order);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				} else {
					System.out.println("El pedido #" + order.getId() + " ya tenía la marca. Saltando...");
				}
			}
			System.out.println("--- ✅ MARCADO MASIVO FINALIZADO ---");
			System.exit(0);*/



			String claveTelegram = "_aviso_telegram_preparado";

			System.out.println("--- REVISANDO PEDIDOS PREPARADOS ---");
			List<PedidoDTO> ordersPreparedCocex = wooService.obtenerPedidos("prepared-cocex");

			for (PedidoDTO order: ordersPreparedCocex) {

				boolean yaAvisado = wooService.comprobarAccionMetaData(order, claveTelegram);

				if (!yaAvisado) {
					System.out.println("🔔 Notificando (_aviso_telegram_preparado) al cliente del pedido #" + order.getId());

					wooService.actualizarMetaData(order, claveTelegram, "true");

					telegramService.notificarPedidoPreparado(order);
					continue;
				}

				CorreosInfo info = correosScraperService.obtenerEstadoActual(order.getTrackingNumber());

				long diasSinMoverse = ChronoUnit.DAYS.between(info.fechaEvento(), LocalDate.now());
				String estadoMayus = info.estado().toUpperCase();

				// SEGURIDAD POR SI NOS BLOQUEAN
				if (estadoMayus.equals("BLOQUEO_IP")) {
					System.err.println("⚠️ Deteniendo la ejecución por bloqueo de IP...");
					telegramService.alertarBloqueoIP();
					break;
				}

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
				else if (estadoMayus.contains("SE ENCUENTRA EN LA OFICINA DE CORREOS") || estadoMayus.contains("DISPOSICIÓN")) {
					telegramService.notificarPedidoDisposicion(order);
				}
				// D) Está parado
				else if (estadoMayus.contains("PARADO")) {
					System.err.println("⚠️ PEDIDO PARADO: PENDIENTE DE INDICACIONES");
					telegramService.alertarPedidoEstacionado(order);

				// E) Está prerregistrado desde hace días y no se ha movido
				} else if (estadoMayus.contains("PRERREGISTRADO") && diasSinMoverse > 3) {
					telegramService.alertarPedidoNoEnviado(order, estadoMayus, diasSinMoverse);
				}

				// Pausa aleatoria entre peticiones
				try {
					// Genera un tiempo aleatorio entre 3 y 7 segundos (3000ms a 7000ms)
					long sleepTime = 3000 + (long) (Math.random() * 4000);
					System.out.println("⏳ Esperando " + (sleepTime / 1000.0) + " segundos antes de consultar el siguiente pedido...");
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					System.err.println("Error durante la pausa de seguridad: " + e.getMessage());
					Thread.currentThread().interrupt();
				}
			}

			System.out.println("------------------------ FINALIZADO -------------------------------");

			//System.exit(0);
		};
	}

}
