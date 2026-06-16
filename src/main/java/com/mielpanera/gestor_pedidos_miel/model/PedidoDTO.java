package com.mielpanera.gestor_pedidos_miel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PedidoDTO {
    private int id;
    private int number;
    private String status;
    private Date date_created;
    private Billing billing;
    
    @JsonProperty("correos_tracking_number")
    private String trackingNumber;
    
    @JsonProperty("customer_note")
    private String customerNote;
    
    @JsonProperty("line_items")
    private List<LineProduct> lineItems;
    
    @JsonProperty("meta_data")
    private List<MetaData> metaData;

    @Override
    public String toString() {
        String nombreCliente = (billing != null) ? billing.getNombreCompleto() : "Desconocido";
        String infoSeguimiento = (trackingNumber != null) ? trackingNumber : "Sin asignar";

        return "Pedido #" + id +
                " | Cliente: " + nombreCliente +
                " | Estado: " + status +
                " | Tracking: " + infoSeguimiento;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Billing {
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("phone")
        private String phoneNumber;

        public String getNombreCompleto() {
            String nombre = (firstName != null) ? firstName : "";
            String apellido = (lastName != null) ? lastName : "";
            return (nombre + " " + apellido).trim();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LineProduct {
        private int id;
        @JsonProperty("product_id")
        private int productID;
        private String name;
        private int quantity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaData {
        private String key;
        private Object value;

        public String getValueAsString() {
            return value != null ? value.toString() : "";
        }
    }
}
