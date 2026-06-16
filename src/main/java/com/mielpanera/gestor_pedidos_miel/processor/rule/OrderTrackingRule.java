package com.mielpanera.gestor_pedidos_miel.processor.rule;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;

public interface OrderTrackingRule {
    boolean evalua(PedidoDTO pedido, TrackingStatusCategory categoria, long diasSinMoverse);
    void ejecuta(PedidoDTO pedido, CorreosInfo trackingInfo, long diasSinMoverse);
}
