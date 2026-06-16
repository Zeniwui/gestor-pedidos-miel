package com.mielpanera.gestor_pedidos_miel.model;

public enum TrackingStatusCategory {
    IP_BLOCKED,
    DELIVERED,
    RETURNED,
    READY_FOR_PICKUP,
    STUCK,
    PRE_REGISTERED,
    UNKNOWN;

    public static TrackingStatusCategory fromStatusText(String statusText) {
        if (statusText == null) {
            return UNKNOWN;
        }
        String upper = statusText.toUpperCase();
        if (upper.equals("BLOQUEO_IP")) {
            return IP_BLOCKED;
        }
        if (upper.contains("ENTREGADO")) {
            return DELIVERED;
        }
        if (upper.contains("DEVOLUCIÓN") || upper.contains("DEVUELTO")) {
            return RETURNED;
        }
        if (upper.contains("SE ENCUENTRA EN LA OFICINA DE CORREOS") || upper.contains("DISPOSICIÓN")) {
            return READY_FOR_PICKUP;
        }
        if (upper.contains("PARADO")) {
            return STUCK;
        }
        if (upper.contains("PRERREGISTRADO")) {
            return PRE_REGISTERED;
        }
        return UNKNOWN;
    }
}
