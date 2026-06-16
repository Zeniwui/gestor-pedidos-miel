package com.mielpanera.gestor_pedidos_miel.processor;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.processor.rule.TrackingRuleEngine;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import com.mielpanera.gestor_pedidos_miel.service.TrackingService;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class OrderProcessor {

    private final WooCommerceService wooService;
    private final TrackingService trackingService;
    private final TelegramService telegramService;
    private final TrackingRuleEngine ruleEngine;

    public OrderProcessor(WooCommerceService wooService,
                          TrackingService trackingService,
                          TelegramService telegramService,
                          TrackingRuleEngine ruleEngine) {
        this.wooService = wooService;
        this.trackingService = trackingService;
        this.telegramService = telegramService;
        this.ruleEngine = ruleEngine;
    }

    public void procesarPedidos() {
        String claveTelegram = "_aviso_telegram_preparado";

        System.out.println("--- REVISANDO PEDIDOS PREPARADOS ---");
        List<PedidoDTO> ordersPreparedCocex;
        try {
            ordersPreparedCocex = wooService.obtenerPedidos("prepared-cocex");
        } catch (Exception e) {
            System.err.println("❌ Error crítico al obtener pedidos de WooCommerce: " + e.getMessage());
            return;
        }

        for (PedidoDTO order : ordersPreparedCocex) {
            try {
                boolean yaAvisado = wooService.comprobarAccionMetaData(order, claveTelegram);

                if (!yaAvisado) {
                    System.out.println("🔔 Notificando (_aviso_telegram_preparado) al cliente del pedido #" + order.getId());
                    wooService.actualizarMetaData(order, claveTelegram, "true");
                    telegramService.notificarPedidoPreparado(order);
                    continue;
                }

                CorreosInfo info = trackingService.obtenerEstadoActual(order.getTrackingNumber());
                if (info == null) {
                    System.err.println("⚠️ No se pudo obtener información de tracking para el pedido #" + order.getId());
                    continue;
                }

                String estadoMayus = info.estado() != null ? info.estado().toUpperCase() : "DESCONOCIDO";

                // SEGURIDAD POR SI NOS BLOQUEAN IP
                if ("BLOQUEO_IP".equals(estadoMayus)) {
                    System.err.println("⚠️ Deteniendo la ejecución por bloqueo de IP...");
                    telegramService.alertarBloqueoIP();
                    break;
                }

                long diasSinMoverse = 0;
                if (info.fechaEvento() != null) {
                    diasSinMoverse = ChronoUnit.DAYS.between(info.fechaEvento(), LocalDate.now());
                }

                System.out.printf("📦 ID: %-5s | 👤 %-15s | 🚚 %-15s | 📅 %-10s (+%d días) | ℹ️ %s%n",
                        order.getId(),
                        (order.getBilling() != null) ? order.getBilling().getNombreCompleto() : "Desconocido",
                        order.getTrackingNumber(),
                        info.fechaEvento(),
                        diasSinMoverse,
                        info.estado()
                );

                // Evaluar y ejecutar reglas usando el RuleEngine
                ruleEngine.procesar(order, info, diasSinMoverse);

                // Pausa aleatoria entre peticiones para evitar bloqueos
                esperarSeguridad();

            } catch (Exception e) {
                System.err.println("❌ Error procesando el pedido #" + order.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("------------------------ FINALIZADO -------------------------------");
    }

    private void esperarSeguridad() {
        try {
            long sleepTime = 3000 + (long) (Math.random() * 4000);
            System.out.println("⏳ Esperando " + (sleepTime / 1000.0) + " segundos antes de consultar el siguiente pedido...");
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            System.err.println("Error durante la pausa de seguridad: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
