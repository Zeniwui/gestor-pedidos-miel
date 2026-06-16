package com.mielpanera.gestor_pedidos_miel.service;

import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;

public interface TelegramService {
    void notificarPedidoEntregado(PedidoDTO order);
    void notificarPedidoPreparado(PedidoDTO order);
    void notificarPedidoDisposicion(PedidoDTO order);
    void alertarPedidoEstacionado(PedidoDTO order);
    void alertarPedidoAtascado(PedidoDTO order, String estado, long diasSinMoverse);
    void alertarPedidoNoEnviado(PedidoDTO order, String estado, long diasSinMoverse);
    void alertarBloqueoIP();
}
