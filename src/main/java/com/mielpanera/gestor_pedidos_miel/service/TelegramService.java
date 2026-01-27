package com.mielpanera.gestor_pedidos_miel.service;

import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TelegramService {

    private final String telegramToken = System.getenv("TELEGRAM_TOKEN");
    private final String telegramChatID = System.getenv("TELEGRAM_CHAT_ID");

    private final RestClient restClient;

    public TelegramService(RestClient restClient) {
        this.restClient = restClient;
    }

    public void notificarPedidoEntregado(PedidoDTO order) {
        String phone = order.getBilling().getPhoneNumber();
        String nombre = order.getBilling().getFirstName();

        String telefonoLimpio = limpiarTelefono(phone);

        String mensajeCliente = "¡Hola " + nombre + "! 🐝 Tu pedido de miel #" + order.getId() +
                " ha sido entregado. ¡Esperamos que lo disfrutes! 🍯";

        String waLink = "https://wa.me/" + telefonoLimpio + "?text=" +
                URLEncoder.encode(mensajeCliente, StandardCharsets.UTF_8);

        String mensajeTelegram = "<b>📦 PEDIDO #" + order.getId() + " ENTREGADO</b>\n" +
                "Cliente: " + nombre + "\n" +
                "Estado: Completado en Woo ✅\n\n" +
                "👇 <b>Pulsa aquí para avisar al cliente:</b>\n" +
                "<a href=\"" + waLink + "\">📲 Enviar WhatsApp</a>";

        enviarMensajeTelegram(mensajeTelegram);
    }

    public void notificarPedidoPreparado(PedidoDTO order) {
        // 1. Extraemos datos básicos
        String phone = order.getBilling().getPhoneNumber();
        String nombre = order.getBilling().getFirstName();
        String trackingNumber = order.getTrackingNumber();

        String telefonoLimpio = limpiarTelefono(phone);

        String correosLink = "https://www.correos.es/es/es/herramientas/localizador/envios/detalle?tracking-number=" + trackingNumber;

        String mensajeCliente = "¡Hola " + nombre + "! 🐝 Tu pedido de miel #" + order.getId() +
                " ya ha sido preparado y listo para entregar a Correos. 🚚\n\n" +
                "Puedes seguir el estado de tu paquete aquí:\n" +
                correosLink + "\n\n" +
                "¡Muchas gracias por tu confianza!";

        String waLink = "";
        try {
            waLink = "https://wa.me/" + telefonoLimpio + "?text=" +
                    URLEncoder.encode(mensajeCliente, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            System.err.println("Error codificando URL: " + e.getMessage());
            return;
        }

        String mensajeTelegram = "<b>🚚 PEDIDO #" + order.getId() + " PREPARADO</b>\n" +
                "Cliente: " + nombre + "\n" +
                "Tracking: <code>" + trackingNumber + "</code>\n" +
                "Estado: Listo para transporte 🟡\n\n" +
                "👇 <b>Pulsa aquí para enviar el tracking:</b>\n" +
                "<a href=\"" + waLink + "\">📲 Enviar WhatsApp con Tracking</a>";

        // 6. Enviamos a tu Telegram
        enviarMensajeTelegram(mensajeTelegram);
    }


    public void notificarPedidoDisposicion(PedidoDTO pedido) {
        String phone = pedido.getBilling().getPhoneNumber();
        String nombre = pedido.getBilling().getFirstName();

        String telefonoLimpio = limpiarTelefono(phone);

        String correosLink = "https://www.correos.es/es/es/herramientas/localizador/envios/detalle?tracking-number=" +
                pedido.getTrackingNumber();

        String mensajeCliente = "¡Hola " + nombre + "! 📦 Desde Correos nos informan que tienen tu pedido de Miel Panera para recoger en la oficina 🍯" +
                "\nDesde este link puedes encontrar toda la información relacionada con tu pedido:\n" +
                correosLink;

        String waLink = "https://wa.me/" + telefonoLimpio + "?text=" +
                URLEncoder.encode(mensajeCliente, StandardCharsets.UTF_8);

        String mensajeTelegram = "<b>📦 PEDIDO #" + pedido.getId() + " A DISPOSICIÓN DEL DESTINATARIO</b>\n" +
                "Cliente: " + nombre + "\n" +
                "Estado: A disposición del destinatario\n\n" +
                "👇 <b>Pulsa aquí para avisar al cliente:</b>\n" +
                "<a href=\"" + waLink + "\">📲 Enviar WhatsApp</a>";

        enviarMensajeTelegram(mensajeTelegram);
    }

    public void notificarPedidoAtascado(PedidoDTO pedido, String estado, long diasSinMoverse) {
        String mensaje = "⚠️ <b>ALERTA DE PEDIDO ATASCADO</b> ⚠️\n\n" +
                "📦 <b>Pedido:</b> #" + pedido.getId() + "\n" +
                "👤 <b>Cliente:</b> " + pedido.getBilling().getNombreCompleto() + "\n" +
                "🚚 <b>Estado Correos:</b> " + estado + "\n" +
                "⏳ <b>Días sin cambios:</b> " + diasSinMoverse + " días\n\n" +
                "🔍 <i>Revisa la web de Correos manualmente.</i>";

        enviarMensajeTelegram(mensaje);
    }

    public void enviarMensajeTelegram(String mensajeHtml) {
        try {
            String url = "https://api.telegram.org/bot" + telegramToken + "/sendMessage";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("chat_id", telegramChatID);
            body.add("text", mensajeHtml);
            body.add("parse_mode", "HTML"); // Importante para que funcione el enlace

            restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("📩 Notificación enviada a Telegram.");

        } catch (Exception e) {
            System.err.println("Error enviando Telegram: " + e.getMessage());
        }
    }

    public String limpiarTelefono(String telefono) {
        if (!telefono.startsWith("34") && telefono.length() == 9) {
            return "34" + telefono;
        }
        return telefono;
    }

}
