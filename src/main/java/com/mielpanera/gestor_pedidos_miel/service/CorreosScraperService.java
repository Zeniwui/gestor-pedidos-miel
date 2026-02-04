package com.mielpanera.gestor_pedidos_miel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class CorreosScraperService {

    // Esta es la URL interna que usa la web de Correos (descubierta con F12)
    private static final String API_OCULTA_URL = "https://api1.correos.es/digital-services/searchengines/api/v1/envios?text={TRACKING}&language=ES";
    private static final DateTimeFormatter FORMATO_FECHA_CORREOS = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CorreosInfo obtenerEstadoActual(String trackingId) {
        try {
            // 1. Construimos la URL
            if (trackingId == null || trackingId.trim().isEmpty()) {
                System.err.println("Error: Se ha intentado consultar un Tracking ID nulo o vacío.");
                return new CorreosInfo("ID_INVALIDO", LocalDate.now());
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
                return new CorreosInfo("NOT_FOUND", LocalDate.now());
            }
            return new CorreosInfo("ERROR_HTTP", LocalDate.now());
        } catch (IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return new CorreosInfo("ERROR_CONEXION", LocalDate.now());
        }
    }

    private CorreosInfo extraerUltimoEstado(String json) {
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

                String estadoTexto = "DESCONOCIDO";
                if (eventsArray != null && !eventsArray.isEmpty()) {
                    JsonNode lastEvent = eventsArray.get(eventsArray.size()-1);

                    if (lastEvent.has("extendedText")) {
                        estadoTexto = lastEvent.get("desPhase").asText();
                    } else if (lastEvent.has("summaryText")) {
                        estadoTexto = lastEvent.get("summaryText").asText();
                    } else if (lastEvent.has("extendedText")) {
                        estadoTexto = lastEvent.get("extendedText").asText();
                    }

                    // B) Extraer la FECHA
                    LocalDate fechaEvento = LocalDate.now(); // Por defecto hoy si falla
                    if (lastEvent.has("eventDate")) {
                        String fechaStr = lastEvent.get("eventDate").asText(); // "16/01/2026"
                        try {
                            fechaEvento = LocalDate.parse(fechaStr, FORMATO_FECHA_CORREOS);
                        } catch (Exception e) {
                            System.err.println("No se pudo parsear la fecha: " + fechaStr);
                        }
                    }
                    return new CorreosInfo(estadoTexto, fechaEvento);
                }
            }
            return new CorreosInfo("DESCONOCIDO - SIN EVENTOS", LocalDate.now());
        } catch (Exception e) {
            System.err.println("Error parseando JSON Correos: " + e.getMessage());
            return new CorreosInfo("ERROR_PARSING", LocalDate.now());
        }
    }
}
