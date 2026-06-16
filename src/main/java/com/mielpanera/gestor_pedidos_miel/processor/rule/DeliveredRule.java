package com.mielpanera.gestor_pedidos_miel.processor.rule;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import org.springframework.stereotype.Component;

@Component
public class DeliveredRule implements OrderTrackingRule {

    private final WooCommerceService wooService;

    public DeliveredRule(WooCommerceService wooService) {
        this.wooService = wooService;
    }

    @Override
    public boolean evalua(PedidoDTO pedido, TrackingStatusCategory categoria, long diasSinMoverse) {
        return categoria == TrackingStatusCategory.DELIVERED;
    }

    @Override
    public void ejecuta(PedidoDTO pedido, CorreosInfo trackingInfo, long diasSinMoverse) {
        wooService.actualizarEstadoPedido(pedido, "completed");
    }
}
