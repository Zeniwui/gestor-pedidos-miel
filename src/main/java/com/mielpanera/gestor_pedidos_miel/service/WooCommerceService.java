package com.mielpanera.gestor_pedidos_miel.service;

import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import java.util.List;

public interface WooCommerceService {
    List<PedidoDTO> obtenerPedidos(String status);
    PedidoDTO actualizarEstadoPedido(PedidoDTO order, String newStatus);
    void addOrderNote(PedidoDTO order, String noteContent, boolean isCustomerNote);
    boolean verifyOrderNote(PedidoDTO order, String searchString);
    void addProductsInObservations(PedidoDTO order);
    boolean comprobarAccionMetaData(PedidoDTO order, String metaKey);
    void actualizarMetaData(PedidoDTO order, String metaKey, String metaValue);
}
