package com.mielpanera.gestor_pedidos_miel.processor.rule;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import org.springframework.stereotype.Component;

@Component
public class ReadyForPickupRule implements OrderTrackingRule {

    private final TelegramService telegramService;

    public ReadyForPickupRule(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public boolean evalua(PedidoDTO pedido, TrackingStatusCategory categoria, long diasSinMoverse) {
        return categoria == TrackingStatusCategory.READY_FOR_PICKUP;
    }

    @Override
    public void ejecuta(PedidoDTO pedido, CorreosInfo trackingInfo, long diasSinMoverse) {
        telegramService.notificarPedidoDisposicion(pedido);
    }
}
