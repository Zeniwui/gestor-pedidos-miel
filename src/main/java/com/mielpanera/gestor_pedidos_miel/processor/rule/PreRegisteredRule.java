package com.mielpanera.gestor_pedidos_miel.processor.rule;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import org.springframework.stereotype.Component;

@Component
public class PreRegisteredRule implements OrderTrackingRule {

    private final TelegramService telegramService;

    public PreRegisteredRule(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Override
    public boolean evalua(PedidoDTO pedido, TrackingStatusCategory categoria, long diasSinMoverse) {
        return categoria == TrackingStatusCategory.PRE_REGISTERED && diasSinMoverse > 3;
    }

    @Override
    public void ejecuta(PedidoDTO pedido, CorreosInfo trackingInfo, long diasSinMoverse) {
        telegramService.alertarPedidoNoEnviado(pedido, trackingInfo.estado().toUpperCase(), diasSinMoverse);
    }
}
