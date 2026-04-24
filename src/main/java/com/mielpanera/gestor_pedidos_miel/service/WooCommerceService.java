package com.mielpanera.gestor_pedidos_miel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class WooCommerceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String STORE_URL = System.getenv("STORE_URL");
    private final String CONSUMER_KEY = System.getenv("WOO_CONSUMER_KEY");
    private final String CONSUMER_SECRET = System.getenv("WOO_CONSUMER_SECRET");

    public WooCommerceService(RestTemplateBuilder builder, ObjectMapper mapper) {
        if (CONSUMER_KEY == null || CONSUMER_SECRET == null) {
            throw new IllegalStateException("FATAL: Las variables de entorno no están configuradas.");
        }

        this.restTemplate = builder.build();
        this.objectMapper = mapper;
    }

    public List<PedidoDTO> obtenerPedidos(String status) {

        List<PedidoDTO> allOrders = new ArrayList<>();
        int page = 1;
        boolean hasMoreOrders = true;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        while (hasMoreOrders) {
            String urlFinal = UriComponentsBuilder.fromHttpUrl(STORE_URL + "/wp-json/wc/v3/orders")
                    .queryParam("status", status)
                    .queryParam("per_page", 100)
                    .queryParam("page", page)
                    .toUriString();

            try {
                System.out.println("Consultado Woocommerce...");

                ResponseEntity<List<PedidoDTO>> response = restTemplate.exchange(
                        urlFinal,
                        HttpMethod.GET,
                        entity,
                        new ParameterizedTypeReference<List<PedidoDTO>>() {
                        }
                );

                List<PedidoDTO> ordersInPage = response.getBody();

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
                hasMoreOrders = false;
            }
        }

        System.out.println("Número de pedidos en estado: prepared-cocex: " + allOrders.size());

        return allOrders;
    }

    public PedidoDTO actualizarEstadoPedido(PedidoDTO order, String newStatus) {

        System.out.println("Actualizando pedido...");

        String url = STORE_URL + "/wp-json/wc/v3/orders/" + order.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("status", newStatus);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<PedidoDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    PedidoDTO.class
            );
            System.out.println("Pedido actualizado");

            String note = "API: Estado de pedido actualizado a: " + newStatus;
            addOrderNote(order, note, false);

            return response.getBody();

        } catch (HttpClientErrorException e) {
        System.err.println("Error al actualizar el pedido " + order.getId() + ": " + e.getResponseBodyAsString());
        throw e;
        }
    }

    public void addOrderNote(PedidoDTO order, String noteContent, boolean isCustomerNote) {

        String url = STORE_URL + "/wp-json/wc/v3/orders/" + order.getId() + "/notes";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note", noteContent);
        noteData.put("customer_note", isCustomerNote);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(noteData, headers);

        try {
            restTemplate.postForObject(url, entity, Map.class);
            System.out.println("Nota añadida correctamente al pedido " + order.getId());

        } catch (Exception e) {
            System.err.println("Error al añadir nota al pedido " + order.getId() + ": " + e.getMessage());
        }
    }

    public boolean verifyOrderNote(PedidoDTO order, String searchString) {

        String url = STORE_URL + "/wp-json/wc/v3/orders/" + order.getId() + "/notes?per_page=100";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map[].class
            );

            Map[] notes = response.getBody();

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

    public void addProductsInObservations(PedidoDTO order) {

        if (!verifyOrderNote(order, "observationAdded=true")) {

            String productsAddNote = order.getCustomerNote() + "\n" + productLineParser(order);

            System.out.println(productsAddNote);

            String url = STORE_URL + "/wp-json/wc/v3/orders/" + order.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> noteData = new HashMap<>();
            noteData.put("customer_note", productsAddNote);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(noteData, headers);

            try {
                restTemplate.postForObject(url, entity, Map.class);

                String observationAddedConfirmation = "observationAdded=true";
                addOrderNote(order, observationAddedConfirmation, false);

                System.out.println("Observación añadida correctamente al pedido " + order.getId());


            } catch (Exception e) {
                System.err.println("Error al añadir observación al pedido " + order.getId() + ": " + e.getMessage());
            }
        }

    }

    public void getOrderJson(int orderID) {
        String url = STORE_URL + "/wp-json/wc/v3/orders/" + orderID;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            Object jsonObject = objectMapper.readValue(response.getBody(), Object.class);

            String jsonLegible = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);

            System.out.println(jsonLegible);

        } catch (Exception e) {
            throw new RuntimeException("Error al obtener el pedido de WooCommerce: " + e.getMessage());
        }
    }

    public String productLineParser(PedidoDTO order) {

        Map<Integer, String> PRODUCT_LABELS = Map.ofEntries(
                Map.entry(48, "Bos "),
                Map.entry(50, "Br "),
                Map.entry(61, "x(3TL-3Bos) "),
                Map.entry(62, "x(6Br) "),
                Map.entry(63, "x(6Bos) "),
                Map.entry(143, "Tintu "),
                Map.entry(145, "x(3Bos-3Br) "),
                Map.entry(146, "Polen "),
                Map.entry(242, "TL "),
                Map.entry(331, "Cant "),
                Map.entry(339, "x(6Cant) "),
                Map.entry(374, "x(6TL) "),
                Map.entry(384, "Palito "),
                Map.entry(393, "x(2Br-2Bos-2Cant) "),
                Map.entry(1190, "x(2Br-2Bos-2TL) "),
                Map.entry(1861, "PropoleoCrudo "),
                Map.entry(1863, "Cera "),
                Map.entry(1865, "Cesta ")
        );


        StringBuilder noteParsed = new StringBuilder();

        for (PedidoDTO.LineProduct product : order.getLineItems()) {
            String label = PRODUCT_LABELS.get(product.getProductID());

            if (label != null) {
                noteParsed.append(product.getQuantity())
                        .append(label);
            }
        }

        return noteParsed.toString();
    }
    public boolean comprobarAccionMetaData(PedidoDTO order, String metaKey) {
        if (order.getMetaData() == null || order.getMetaData().isEmpty()) {
            return false; // Si no hay meta datos, no se ha hecho la acción
        }

        for (PedidoDTO.MetaData meta : order.getMetaData()) {
            if (metaKey.equals(meta.getKey())) {
                String valor = meta.getValueAsString();
                // Verificamos si existe y si su valor es true o yes
                return "true".equalsIgnoreCase(valor) || "yes".equalsIgnoreCase(valor);
            }
        }
        return false;
    }
    public void actualizarMetaData(PedidoDTO order, String metaKey, String metaValue) {
        String url = STORE_URL + "/wp-json/wc/v3/orders/" + order.getId();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, String>> metaDataList = new ArrayList<>();
        Map<String, String> metaDataEntry = new HashMap<>();
        metaDataEntry.put("key", metaKey);
        metaDataEntry.put("value", metaValue);
        metaDataList.add(metaDataEntry);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("meta_data", metaDataList);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );
            System.out.println("✅ Meta dato [" + metaKey + "=" + metaValue + "] guardado en WooCommerce.");

            PedidoDTO.MetaData nuevoMeta = new PedidoDTO.MetaData();
            nuevoMeta.setKey(metaKey);
            nuevoMeta.setValue(metaValue);
            order.getMetaData().add(nuevoMeta);

        } catch (Exception e) {
            System.err.println("❌ Error guardando meta dato en pedido " + order.getId() + ": " + e.getMessage());
        }
    }

    public void imprimirMetaDataVisual(PedidoDTO order) {
        System.out.println("\n=======================================================");
        System.out.println("📊 META DATOS DEL PEDIDO #" + order.getId());
        System.out.println("=======================================================");

        if (order.getMetaData() == null || order.getMetaData().isEmpty()) {
            System.out.println("  ❌ No hay meta datos registrados en este pedido.");
        } else {
            System.out.printf(" %-35s | %s%n", "CLAVE (KEY)", "VALOR (VALUE)");
            System.out.println("-------------------------------------------------------");
            for (PedidoDTO.MetaData meta : order.getMetaData()) {
                String valorStr = meta.getValueAsString();
                if (valorStr.length() > 50) {
                    valorStr = valorStr.substring(0, 47) + "...";
                }
                System.out.printf(" 🔑 %-33s | 🏷️ %s%n", meta.getKey(), valorStr);
            }
        }
        System.out.println("=======================================================\n");
    }

}
