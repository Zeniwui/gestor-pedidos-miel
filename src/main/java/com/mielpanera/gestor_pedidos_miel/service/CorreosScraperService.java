package com.mielpanera.gestor_pedidos_miel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CorreosScraperService {

    // Esta es la URL interna que usa la web de Correos (descubierta con F12)
    private static final String API_OCULTA_URL = "https://api1.correos.es/digital-services/searchengines/api/v1/envios?text={TRACKING}&language=ES";

    public String obtenerEstadoActual(String trackingId) {
        try {
            // 1. Construimos la URL
            if (trackingId == null || trackingId.trim().isEmpty()) {
                System.err.println("Error: Se ha intentado consultar un Tracking ID nulo o vacío.");
                return "ID_NVÁLIDO";
            }

            String url = API_OCULTA_URL.replace("{TRACKING}", trackingId);

            // 2. Usamos Jsoup para hacer la petición HTTP
            // Es CRÍTICO hacerse pasar por un navegador real (User Agent)
            // y añadir el 'Referer' para que Correos crea que venimos de su web.
            /*Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true) // Importante: Correos devuelve JSON, Jsoup espera HTML por defecto
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Referer", "https://www.correos.es/")
                    .header("Origin", "https://www.correos.es")
                    .method(Connection.Method.GET)
                    .execute();
*/
            Connection.Response response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .timeout(10000) // Darle 10 segundos por si acaso
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "es-ES,es;q=0.9")
                    .header("Referer", "https://www.correos.es/")
                    .header("Origin", "https://www.correos.es")
                    .header("Host", "api1.correos.es") // A veces ayuda ser explícito
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-site")
                    .method(Connection.Method.GET)
                    .execute();

            // 3. Obtenemos el cuerpo de la respuesta (el JSON)
            String jsonBody = response.body();

/*            // --- DEBUGGING: ESTA ES LA LÍNEA MÁGICA ---
            System.out.println("--------------------------------------------------");
            System.out.println("JSON RAW CORREOS (" + trackingId + "):");
            System.out.println(jsonBody);
            System.out.println("--------------------------------------------------");
            // -------------------------------------------*/


            // 4. Analizamos el JSON
            // Correos suele devolver una lista de eventos. Queremos el último (el más reciente).
            return extraerUltimoEstado(jsonBody);

        } catch (org.jsoup.HttpStatusException e) {
            if (e.getStatusCode() == 404) {
                return "NOT_FOUND"; // El código no existe
            }
            System.err.println("Error HTTP consultando Correos: " + e.getStatusCode());
            return "ERROR";
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return "ERROR";
        }
    }

    private String extraerUltimoEstado(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            // Entramos en la lista "shipment"
            JsonNode shipmentArray = root.get("shipment");

            if (shipmentArray != null && !shipmentArray.isEmpty()) {
                // Cogemos el primer envío
                JsonNode firstEntry = shipmentArray.get(0);

                // Buscamos la lista "events" dentro del envio
                JsonNode eventsArray = firstEntry.get("events");

                if (eventsArray != null && !eventsArray.isEmpty()) {
                    JsonNode lastEvent = eventsArray.get(eventsArray.size()-1);

                    if (lastEvent.has("extendedText")) {
                        return lastEvent.get("extendedText").asText();
                    } else if (lastEvent.has("summaryText")) {
                        return lastEvent.get("summaryText").asText();
                    } else if (lastEvent.has("desPhase")) {
                        return lastEvent.get("desPhase").asText();
                    }
                }
            }

            return "DESCONOCIDO - SIN EVENTOS";

        } catch (Exception e) {
            System.err.println("Error parseando JSON Correos: " + e.getMessage());
            return "ERROR_PARSING";
        }
    }
}
