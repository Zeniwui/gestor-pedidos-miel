package com.mielpanera.gestor_pedidos_miel.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.List;

// Esta etiqueta indica que si vienen datos extra en el JSON del pedido que no haya definido, ignóralos, no lances error
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

    public List<LineProduct> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<LineProduct> lineItems) {
        this.lineItems = lineItems;
    }

    // CONSTRUCTOR VACÍO (Necesario para Spring)
    public PedidoDTO() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDate_created() {
        return date_created;
    }

    public void setDate_created(Date date_created) {
        this.date_created = date_created;
    }

    public Billing getBilling() { return billing; }
    public void setBilling(Billing billing) { this.billing = billing; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String note) { this.customerNote = note; }

    @Override
    public String toString() {
        String nombreCliente = (billing != null) ? billing.getNombreCompleto() : "Desconocido";

        String infoSeguimiento = (trackingNumber != null) ? trackingNumber : "Sin asignar";

        return "Pedido #" + id +
                " | Cliente: " + nombreCliente +
                " | Estado: " + status +
                " | Tracking: " + infoSeguimiento;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Billing {
        @JsonProperty("first_name")
        private String firstName;
        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("phone")
        private String phoneNumber;

        public String getFirstName() {
            return firstName;
        }
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String newPhone) { this.phoneNumber = newPhone; }

        public String getNombreCompleto() {
            // Manejo seguro por si vienen nulos
            String nombre = (firstName != null) ? firstName : "";
            String apellido = (lastName != null) ? lastName : "";
            return (nombre + " " + apellido).trim();
        }
    }

    public static class LineProduct {
        private int id;
        @JsonProperty("product_id")
        private int productID;
        private String name;
        private int quantity;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public int getProductID() { return productID; }
        public void setProductID(int id) { this.productID = id; }

    }
}
