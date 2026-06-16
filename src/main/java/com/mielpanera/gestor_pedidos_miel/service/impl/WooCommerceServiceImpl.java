package com.mielpanera.gestor_pedidos_miel.service.impl;

import com.mielpanera.gestor_pedidos_miel.config.CorreosProperties;
import com.mielpanera.gestor_pedidos_miel.config.WooCommerceProperties;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class WooCommerceServiceImpl implements WooCommerceService {

    private final RestClient restClient;
    private final WooCommerceProperties properties;
    private final CorreosProperties correosProperties;

    private static final List<String> USER_AGENTS = Arrays.asList(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:124.0) Gecko/20100101 Firefox/124.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Safari/605.1.15",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0"
    );
    private static final Random RANDOM = new Random();

    public WooCommerceServiceImpl(RestClient.Builder restClientBuilder, 
                                  WooCommerceProperties properties, 
                                  CorreosProperties correosProperties) {
        this.properties = properties;
        this.correosProperties = correosProperties;

        String storeUrl = properties.getStoreUrl();
        if (storeUrl == null || properties.getConsumerKey() == null || properties.getConsumerSecret() == null) {
            throw new IllegalStateException("FATAL: Las variables de configuración de WooCommerce no están configuradas correctamente.");
        }

        // Normalizar URL (quitar barra diagonal al final si existe)
        String normalizedStoreUrl = storeUrl.endsWith("/") ? storeUrl.substring(0, storeUrl.length() - 1) : storeUrl;

        this.restClient = restClientBuilder
                .baseUrl(normalizedStoreUrl)
                .defaultHeaders(headers -> {
                    headers.setBasicAuth(properties.getConsumerKey(), properties.getConsumerSecret());
                    headers.set("Accept", "application/json, text/plain, */*");
                    headers.set("Accept-Language", "es-ES,es;q=0.9");
                })
                .build();
    }

    private HttpHeaders crearHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size())));
        headers.set("Referer", properties.getStoreUrl());
        headers.set("Origin", properties.getStoreUrl());
        headers.set("Sec-Fetch-Dest", "empty");
        headers.set("Sec-Fetch-Mode", "cors");
        headers.set("Sec-Fetch-Site", "same-origin");
        return headers;
    }

    @Override
    public List<PedidoDTO> obtenerPedidos(String status) {
        List<PedidoDTO> allOrders = new ArrayList<>();
        int page = 1;
        boolean hasMoreOrders = true;

        while (hasMoreOrders) {
            try {
                final int currentPage = page;
                System.out.println("Consultando Woocommerce (Página " + currentPage + ")...");

                List<PedidoDTO> ordersInPage = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/wp-json/wc/v3/orders")
                                .queryParam("status", status)
                                .queryParam("per_page", 100)
                                .queryParam("page", currentPage)
                                .build())
                        .headers(headers -> headers.addAll(crearHeaders()))
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<PedidoDTO>>() {});

                if (ordersInPage != null && !ordersInPage.isEmpty()) {
                    allOrders.addAll(ordersInPage);
                    if (ordersInPage.size() < 100) {
                        hasMoreOrders = false;
                    } else {
                        page++;
                    }
                } else {
                    hasMoreOrders = false;
                }
            } catch (Exception e) {
                System.err.println("Error obteniendo página " + page + ": " + e.getMessage());
                if (page == 1) {
                    throw new RuntimeException("Error fatal en la primera petición a WooCommerce: " + e.getMessage(), e);
                }
                hasMoreOrders = false;
            }
        }

        System.out.println("Número de pedidos en estado " + status + ": " + allOrders.size());
        return allOrders;
    }

    @Override
    public PedidoDTO actualizarEstadoPedido(PedidoDTO order, String newStatus) {
        System.out.println("Actualizando pedido #" + order.getId() + " a estado " + newStatus + "...");

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", newStatus);

        try {
            PedidoDTO response = restClient.put()
                    .uri("/wp-json/wc/v3/orders/{id}", order.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.addAll(crearHeaders()))
                    .body(requestBody)
                    .retrieve()
                    .body(PedidoDTO.class);

            System.out.println("Pedido #" + order.getId() + " actualizado");

            String note = "API: Estado de pedido actualizado a: " + newStatus;
            addOrderNote(order, note, false);

            return response;
        } catch (HttpClientErrorException e) {
            System.err.println("Error al actualizar el pedido " + order.getId() + ": " + e.getResponseBodyAsString());
            throw e;
        }
    }

    @Override
    public void addOrderNote(PedidoDTO order, String noteContent, boolean isCustomerNote) {
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note", noteContent);
        noteData.put("customer_note", isCustomerNote);

        try {
            restClient.post()
                    .uri("/wp-json/wc/v3/orders/{id}/notes", order.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.addAll(crearHeaders()))
                    .body(noteData)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Nota añadida correctamente al pedido " + order.getId());
        } catch (Exception e) {
            System.err.println("Error al añadir nota al pedido " + order.getId() + ": " + e.getMessage());
        }
    }

    @Override
    public boolean verifyOrderNote(PedidoDTO order, String searchString) {
        try {
            Map[] notes = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/wp-json/wc/v3/orders/{id}/notes")
                            .queryParam("per_page", 100)
                            .build(order.getId()))
                    .headers(headers -> headers.addAll(crearHeaders()))
                    .retrieve()
                    .body(Map[].class);

            if (notes != null) {
                for (Map note : notes) {
                    String content = (String) note.get("note");
                    if (content != null && content.contains(searchString)) {
                        System.out.println("Nota encontrada ID: " + note.get("id"));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error al buscar notas en el pedido " + order.getId() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public void addProductsInObservations(PedidoDTO order) {
        if (!verifyOrderNote(order, "observationAdded=true")) {
            String productsAddNote = (order.getCustomerNote() != null ? order.getCustomerNote() : "") 
                    + "\n" + productLineParser(order);

            System.out.println(productsAddNote);

            Map<String, Object> noteData = new HashMap<>();
            noteData.put("customer_note", productsAddNote);

            try {
                restClient.post()
                        .uri("/wp-json/wc/v3/orders/{id}", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.addAll(crearHeaders()))
                        .body(noteData)
                        .retrieve()
                        .toBodilessEntity();

                String observationAddedConfirmation = "observationAdded=true";
                addOrderNote(order, observationAddedConfirmation, false);

                System.out.println("Observación añadida correctamente al pedido " + order.getId());
            } catch (Exception e) {
                System.err.println("Error al añadir observación al pedido " + order.getId() + ": " + e.getMessage());
            }
        }
    }

    private String productLineParser(PedidoDTO order) {
        StringBuilder noteParsed = new StringBuilder();
        Map<Integer, String> labels = correosProperties.getProductLabels();

        if (order.getLineItems() != null) {
            for (PedidoDTO.LineProduct product : order.getLineItems()) {
                String label = labels.get(product.getProductID());
                if (label != null) {
                    noteParsed.append(product.getQuantity()).append(label);
                }
            }
        }
        return noteParsed.toString();
    }

    @Override
    public boolean comprobarAccionMetaData(PedidoDTO order, String metaKey) {
        if (order.getMetaData() == null || order.getMetaData().isEmpty()) {
            return false;
        }

        for (PedidoDTO.MetaData meta : order.getMetaData()) {
            if (metaKey.equals(meta.getKey())) {
                String valor = meta.getValueAsString();
                return "true".equalsIgnoreCase(valor) || "yes".equalsIgnoreCase(valor);
            }
        }
        return false;
    }

    @Override
    public void actualizarMetaData(PedidoDTO order, String metaKey, String metaValue) {
        List<Map<String, String>> metaDataList = new ArrayList<>();
        Map<String, String> metaDataEntry = new HashMap<>();
        metaDataEntry.put("key", metaKey);
        metaDataEntry.put("value", metaValue);
        metaDataList.add(metaDataEntry);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("meta_data", metaDataList);

        try {
            restClient.put()
                    .uri("/wp-json/wc/v3/orders/{id}", order.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.addAll(crearHeaders()))
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("✅ Meta dato [" + metaKey + "=" + metaValue + "] guardado en WooCommerce.");

            if (order.getMetaData() == null) {
                order.setMetaData(new ArrayList<>());
            }
            PedidoDTO.MetaData nuevoMeta = new PedidoDTO.MetaData();
            nuevoMeta.setKey(metaKey);
            nuevoMeta.setValue(metaValue);
            order.getMetaData().add(nuevoMeta);
        } catch (Exception e) {
            System.err.println("❌ Error guardando meta dato en pedido " + order.getId() + ": " + e.getMessage());
        }
    }
}
