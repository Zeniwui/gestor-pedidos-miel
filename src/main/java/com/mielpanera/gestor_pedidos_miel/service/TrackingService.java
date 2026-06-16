package com.mielpanera.gestor_pedidos_miel.service;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;

public interface TrackingService {
    CorreosInfo obtenerEstadoActual(String trackingId);
}
