package com.mielpanera.gestor_pedidos_miel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mielpanera.gestor_pedidos_miel.dto.PedidoDTO;
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


// Etiqueta que le dice a Spring que cargue esta clase en memoria al arrancar
@Service
public class WooCommerceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String STORE_URL = System.getenv("STORE_URL");
    private final String CONSUMER_KEY = System.getenv("WOO_CONSUMER_KEY");
    private final String CONSUMER_SECRET = System.getenv("WOO_CONSUMER_SECRET");

    public WooCommerceService(RestTemplateBuilder builder, ObjectMapper mapper) {
        // Validación de que las variables de entorno están propiamente configuradas
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

        /*
        // La API de Woo está en /wp-json/wc/v3/orders
        // Añadimos las claves en la URL (es la forma más fácil con Woo sobre HTTPS)
        String urlFinal = STORE_URL + "/wp-json/wc/v3/orders?" +
                "consumer_key=" + CONSUMER_KEY +
                "&consumer_secret=" + CONSUMER_SECRET +
                "&status=prepared-cocex";
         */

        // Headers de autenticación: formas más segura de acceder, ya que no añadimos las claves en la URL
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

                // CAMBIO CLAVE: Usamos ParameterizedTypeReference para mantener el tipo de la Lista
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
                // Loguear el error es importante para saber si falla una página concreta
                System.err.println("Error obteniendo página " + page + ": " + e.getMessage());
                hasMoreOrders = false;
            }
        }

        System.out.println("Número de pedidos en estado: prepared-cocex: " + allOrders.size());

        return allOrders;
    }

    public PedidoDTO actualizarEstadoPedido(int idOrder, String newStatus) {

        System.out.println("Actualizando pedido...");

        String url = STORE_URL + "/wp-json/wc/v3/orders/" + idOrder;

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

            String note = "API: Estado de pedido actualizado automáticamente";
            addOrderNote(idOrder, note, false);

            return response.getBody();

        } catch (HttpClientErrorException e) {
        // Manejo de errores específicos (ej: ID no existe devuelve 404)
        System.err.println("Error al actualizar el pedido " + idOrder + ": " + e.getResponseBodyAsString());
        throw e; // O devuelve null según tu lógica de negocio
        }
    }

    public void addOrderNote(int idOrder, String noteContent, boolean isCustomerNote) {

        String url = STORE_URL + "/wp-json/wc/v3/orders/" + idOrder + "/notes";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(CONSUMER_KEY, CONSUMER_SECRET);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note", noteContent);
        noteData.put("customer_note", isCustomerNote);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(noteData, headers);

        try {
            // No necesitamos mapear la respuesta a un DTO complejo si solo queremos confirmar que se creó.
            // Usamos Map.class para recibir la respuesta genérica.
            restTemplate.postForObject(url, entity, Map.class);
            System.out.println("Nota añadida correctamente al pedido " + idOrder);

        } catch (Exception e) {
            System.err.println("Error al añadir nota al pedido " + idOrder + ": " + e.getMessage());
            // Decidimos no lanzar excepción aquí para no interrumpir el flujo principal si la nota falla
        }
    }

    public void addProductsInObservations(PedidoDTO order) {

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
            System.out.println("Nota añadida correctamente al pedido " + order.getId());

        } catch (Exception e) {
            System.err.println("Error al añadir nota al pedido " + order.getId() + ": " + e.getMessage());
        }
    }

    public String productLineParser(PedidoDTO order) {
        List<PedidoDTO.LineProduct> products = order.getLineItems();
        String noteParsed = "";
        for (PedidoDTO.LineProduct product: products) {
            if (product.getProductID() == 48) {
                noteParsed += product.getQuantity() + "Bos ";
            } else if (product.getProductID() == 50) {
                noteParsed += product.getQuantity() + "Br ";
            } else if (product.getProductID() == 61) {
                noteParsed += product.getQuantity() + "x(3TL-3Bos) ";
            }else if (product.getProductID() == 62) {
                noteParsed += product.getQuantity() + "x(6Br) ";
            } else if (product.getProductID() == 63) {
                noteParsed += product.getQuantity() + "x(6Bos) ";
            } else if (product.getProductID() == 143) {
                noteParsed += product.getQuantity() + "Tintu ";
            } else if (product.getProductID() == 145) {
                noteParsed += product.getQuantity() + "x(3Bos-3Br) ";
            } else if (product.getProductID() == 146) {
                noteParsed += product.getQuantity() + "Polen ";
            }else if (product.getProductID() == 384) {
                noteParsed += product.getQuantity() + "Palito ";
            } else if (product.getProductID() == 242) {
                noteParsed += product.getQuantity() + "TL ";
            } else if (product.getProductID() == 331) {
                noteParsed += product.getQuantity() + "Cant ";
            } else if (product.getProductID() == 339) {
                noteParsed += product.getQuantity() + "x(6Cant) ";
            } else if (product.getProductID() == 374) {
                noteParsed += product.getQuantity() + "x(6TL) ";
            } else if (product.getProductID() == 393) {
                noteParsed += product.getQuantity() + "x(2Br-2Bos-2Cant) ";
            } else if (product.getProductID() == 1190) {
                noteParsed += product.getQuantity() + "x(2Br-2Bos-2TL) ";
            } else if (product.getProductID() == 1861) {
                noteParsed += product.getQuantity() + "PropoleoCrudo ";
            } else if (product.getProductID() == 1863) {
                noteParsed += product.getQuantity() + "Cera ";
            } else if (product.getProductID() == 1865) {
                noteParsed += product.getQuantity() + "Cesta ";
            }

        }

        return noteParsed;
    }

}
