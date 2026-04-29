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
    private final String tokenPreparados = System.getenv("TELEGRAM_PREPARADOS");
    private final String tokenDisposicion = System.getenv("TELEGRAM_DISPOSICION");
    private final String tokenNoEnviados = System.getenv("TELEGRAM_NOENVIADOS");
    private final String tokenAlertas = System.getenv("TELEGRAM_ALERTAS");
    private final String tokenEstacionados = System.getenv("TELEGRAM_ESTACIONADOS");
    private final String telegramChatID = System.getenv("TELEGRAM_CHAT_ID");
    private final String TELEFONOOFICIAL = System.getenv("TELEFONO_OFICIAL");
    private final RestClient restClient;

    public TelegramService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
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

        enviarMensajeTelegram(mensajeTelegram, tokenPreparados);
    }

    public void notificarPedidoPreparado(PedidoDTO order) {
        String phone = order.getBilling().getPhoneNumber();
        String nombre = order.getBilling().getFirstName();
        String trackingNumber = order.getTrackingNumber();

        String telefonoLimpio = limpiarTelefono(phone);

        String correosLink = "https://www.correos.es/es/es/herramientas/localizador/envios/detalle?tracking-number=" + trackingNumber;

        String mensajeCliente = "¡Hola " + nombre + "! 🐝 Tu pedido de miel Nº" + order.getId() +
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

        enviarMensajeTelegram(mensajeTelegram, tokenPreparados);
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

        enviarMensajeTelegram(mensajeTelegram, tokenDisposicion);
    }

    public void alertarPedidoEstacionado(PedidoDTO pedido) {
        String mensaje = "⚠️ <b>ALERTA DE PEDIDO ESTACIONADO</b> ⚠️\n\n" +
                "📦 <b>Pedido:</b> #" + pedido.getId() + "\n" +
                "👤 <b>Cliente:</b> " + pedido.getBilling().getNombreCompleto() + "\n" +
                "🚚 <b>Estado Correos:</b> " + "ESTACIONADO" + "\n" +
                "🔍 <i>Revisa la web de Correos manualmente.</i>";

        enviarMensajeTelegram(mensaje, tokenEstacionados);
    }

    public void alertarPedidoAtascado(PedidoDTO pedido, String estado, long diasSinMoverse) {
        String mensaje = "⚠️ <b>ALERTA DE PEDIDO ATASCADO</b> ⚠️\n\n" +
                "📦 <b>Pedido:</b> #" + pedido.getId() + "\n" +
                "👤 <b>Cliente:</b> " + pedido.getBilling().getNombreCompleto() + "\n" +
                "🚚 <b>Estado Correos:</b> " + estado + "\n" +
                "⏳ <b>Días sin cambios:</b> " + diasSinMoverse + " días\n\n" +
                "🔍 <i>Revisa la web de Correos manualmente.</i>";

        enviarMensajeTelegram(mensaje, telegramToken);
    }

    public void alertarPedidoNoEnviado(PedidoDTO pedido, String estado, long diasSinMoverse) {
        String mensaje = "⚠️ ALERTA DE PEDIDO NO ENVIADO ⚠️\n\n" +
                "📦 Pedido: #" + pedido.getId() + "\n" +
                "👤 Cliente: " + pedido.getBilling().getNombreCompleto() + "\n" +
                "🚚 Estado Correos: " + estado + "\n" +
                "⏳ Días sin cambios: " + diasSinMoverse + " días\n\n" +
                "🔍 Revisar por si no se ha entregado a la cartera";

        String waLink = "https://wa.me/" + TELEFONOOFICIAL + "?text=" +
                URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

        String mensajeTelegram = "<b>📦 PEDIDO #" + pedido.getId() + " NO ENVIADO</b>\n" +
                "Cliente: " + pedido.getBilling().getNombreCompleto() + "\n" +
                "Estado: Prerregistrado desde hace días\n\n" +
                "👇 <b>Pulsa aquí para mandar a WhatsApp:</b>\n" +
                "<a href=\"" + waLink + "\">📲 Enviar WhatsApp</a>";
        enviarMensajeTelegram(mensajeTelegram, tokenNoEnviados);
    }

    public void alertarBloqueoIP() {
        String mensaje = "🚨 <b>ALERTA CRÍTICA: BLOQUEO DE IP</b> 🚨\n\n" +
                "El servidor de Correos ha devuelto un error 429 (Too Many Requests) o 403 (Forbidden).\n" +
                "Se ha <b>detenido</b> el escaneo automático para evitar un baneo permanente.\n\n" +
                "🛑 <i>El proceso se ha pausado por seguridad.</i>";

        enviarMensajeTelegram(mensaje, tokenAlertas);
    }

    public void enviarMensajeTelegram(String mensajeHtml, String token) {
        if (token == null || telegramChatID == null) {
            System.err.println("Error: Token o ChatID de Telegram no configurados.");
            return;
        }

        try {
            String url = "https://api.telegram.org/bot" + token + "/sendMessage";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("chat_id", telegramChatID);
            body.add("text", mensajeHtml);
            body.add("parse_mode", "HTML");

            restClient.post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("📩 Notificación enviada a Telegram (Chat: " + telegramChatID + ").");

        } catch (Exception e) {
            System.err.println("Error enviando Telegram: " + e.getMessage());
        }
    }

    public String limpiarTelefono(String telefono) {
        if (telefono == null) {
            return null;
        }

        String telefonoLimpio = telefono.replaceAll("\\s+", "").replace("+", "");

        if (!telefonoLimpio.startsWith("34") && telefonoLimpio.length() == 9) {
            return "34" + telefonoLimpio;
        }

        return telefonoLimpio;
    }

}
