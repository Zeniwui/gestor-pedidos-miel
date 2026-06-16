package com.mielpanera.gestor_pedidos_miel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.service.TrackingService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
public class CorreosScraperTrackingServiceImpl implements TrackingService {

    private static final String API_OCULTA_URL = "https://api1.correos.es/digital-services/searchengines/api/v1/envios?text={TRACKING}&language=ES";
    private static final DateTimeFormatter FORMATO_FECHA_CORREOS = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36"
    );

    private static final Random RANDOM = new Random();
    private final RestClient restClient;
    private final ObjectMapper mapper;

    public CorreosScraperTrackingServiceImpl(RestClient.Builder restClientBuilder, ObjectMapper mapper) {
        this.restClient = restClientBuilder.build();
        this.mapper = mapper;
    }

    @Override
    public CorreosInfo obtenerEstadoActual(String trackingId) {
        try {
            if (trackingId == null || trackingId.trim().isEmpty()) {
                System.err.println("Error: Se ha intentado consultar un Tracking ID nulo o vacío.");
                return new CorreosInfo("ID_INVALIDO", LocalDate.now());
            }

            String url = API_OCULTA_URL.replace("{TRACKING}", trackingId);
            String userAgentAleatorio = USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));

            String jsonBody = restClient.get()
                    .uri(url)
                    .headers(headers -> {
                        headers.set("User-Agent", userAgentAleatorio);
                        headers.set("Accept", "application/json, text/plain, */*");
                        headers.set("Accept-Language", "es-ES,es;q=0.9");
                        headers.set("Referer", "https://www.correos.es/");
                        headers.set("Origin", "https://www.correos.es");
                        headers.set("Host", "api1.correos.es");
                        headers.set("Sec-Fetch-Dest", "empty");
                        headers.set("Sec-Fetch-Mode", "cors");
                        headers.set("Sec-Fetch-Site", "same-site");
                    })
                    .retrieve()
                    .body(String.class);

            return extraerUltimoEstado(jsonBody);

        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 404) {
                return new CorreosInfo("NOT_FOUND", LocalDate.now());
            } else if (statusCode == 429 || statusCode == 403) {
                System.err.println("🚨 BLOQUEO DETECTADO por Correos (HTTP " + statusCode + ")");
                return new CorreosInfo("BLOQUEO_IP", LocalDate.now());
            }
            return new CorreosInfo("ERROR_HTTP", LocalDate.now());
        } catch (Exception e) {
            System.err.println("Error de conexión al consultar tracking: " + e.getMessage());
            return new CorreosInfo("ERROR_CONEXION", LocalDate.now());
        }
    }

    private CorreosInfo extraerUltimoEstado(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode shipmentArray = root.get("shipment");

            if (shipmentArray != null && !shipmentArray.isEmpty()) {
                JsonNode firstEntry = shipmentArray.get(0);
                JsonNode eventsArray = firstEntry.get("events");

                String estadoTexto = "DESCONOCIDO";
                if (eventsArray != null && !eventsArray.isEmpty()) {
                    JsonNode lastEvent = eventsArray.get(eventsArray.size() - 1);

                    if (lastEvent.has("extendedText")) {
                        estadoTexto = lastEvent.get("extendedText").asText();
                    } else if (lastEvent.has("summaryText")) {
                        estadoTexto = lastEvent.get("summaryText").asText();
                    } else if (lastEvent.has("desPhase")) {
                        estadoTexto = lastEvent.get("desPhase").asText();
                    }

                    LocalDate fechaEvento = LocalDate.now();
                    if (lastEvent.has("eventDate")) {
                        String fechaStr = lastEvent.get("eventDate").asText();
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
        } catch (IOException e) {
            System.err.println("Error parseando JSON Correos: " + e.getMessage());
            return new CorreosInfo("ERROR_PARSING", LocalDate.now());
        }
    }
}
