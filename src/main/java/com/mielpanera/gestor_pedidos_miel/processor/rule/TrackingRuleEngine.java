package com.mielpanera.gestor_pedidos_miel.processor.rule;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrackingRuleEngine {

    private final List<OrderTrackingRule> rules;

    public TrackingRuleEngine(List<OrderTrackingRule> rules) {
        this.rules = rules;
    }

    public void procesar(PedidoDTO order, CorreosInfo trackingInfo, long diasSinMoverse) {
        TrackingStatusCategory categoria = TrackingStatusCategory.fromStatusText(trackingInfo.estado());

        for (OrderTrackingRule rule : rules) {
            if (rule.evalua(order, categoria, diasSinMoverse)) {
                rule.ejecuta(order, trackingInfo, diasSinMoverse);
                return; // Solo se ejecuta la primera regla que coincida (emulando el original if-else-if)
            }
        }
    }
}
