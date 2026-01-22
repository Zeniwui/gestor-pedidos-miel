package com.mielpanera.gestor_pedidos_miel.service;

import com.mielpanera.gestor_pedidos_miel.dto.PedidoDTO;
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

        // 3. Generamos el enlace wa.me (codificando espacios y símbolos)
        String waLink = "https://wa.me/" + telefonoLimpio + "?text=" +
                URLEncoder.encode(mensajeCliente, StandardCharsets.UTF_8);

        // 4. Creamos el mensaje para TI (Telegram) usando HTML
        String mensajeTelegram = "<b>📦 PEDIDO #" + order.getId() + " ENTREGADO</b>\n" +
                "Cliente: " + nombre + "\n" +
                "Estado: Completado en Woo ✅\n\n" +
                "👇 <b>Pulsa aquí para avisar al cliente:</b>\n" +
                "<a href=\"" + waLink + "\">📲 Enviar WhatsApp</a>";

        enviarMensajeTelegram(mensajeTelegram);
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
