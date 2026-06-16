package com.mielpanera.gestor_pedidos_miel;

import com.mielpanera.gestor_pedidos_miel.model.CorreosInfo;
import com.mielpanera.gestor_pedidos_miel.model.PedidoDTO;
import com.mielpanera.gestor_pedidos_miel.model.TrackingStatusCategory;
import com.mielpanera.gestor_pedidos_miel.processor.rule.*;
import com.mielpanera.gestor_pedidos_miel.service.TelegramService;
import com.mielpanera.gestor_pedidos_miel.service.WooCommerceService;
import com.mielpanera.gestor_pedidos_miel.service.impl.TelegramServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestorPedidosMielTest {

    private WooCommerceService wooService;
    private TelegramService telegramService;

    @BeforeEach
    void setUp() {
        wooService = mock(WooCommerceService.class);
        telegramService = mock(TelegramService.class);
    }

    @Test
    void testTrackingStatusCategoryParsing() {
        assertEquals(TrackingStatusCategory.IP_BLOCKED, TrackingStatusCategory.fromStatusText("BLOQUEO_IP"));
        assertEquals(TrackingStatusCategory.DELIVERED, TrackingStatusCategory.fromStatusText("El envío ha sido entregado"));
        assertEquals(TrackingStatusCategory.RETURNED, TrackingStatusCategory.fromStatusText("Devolución al remitente"));
        assertEquals(TrackingStatusCategory.RETURNED, TrackingStatusCategory.fromStatusText("Pedido devuelto"));
        assertEquals(TrackingStatusCategory.READY_FOR_PICKUP, TrackingStatusCategory.fromStatusText("Se encuentra en la oficina de correos"));
        assertEquals(TrackingStatusCategory.READY_FOR_PICKUP, TrackingStatusCategory.fromStatusText("Disposición del cliente"));
        assertEquals(TrackingStatusCategory.STUCK, TrackingStatusCategory.fromStatusText("Envío parado por aduanas"));
        assertEquals(TrackingStatusCategory.PRE_REGISTERED, TrackingStatusCategory.fromStatusText("Prerregistrado en el sistema"));
        assertEquals(TrackingStatusCategory.UNKNOWN, TrackingStatusCategory.fromStatusText("En camino"));
        assertEquals(TrackingStatusCategory.UNKNOWN, TrackingStatusCategory.fromStatusText(null));
    }

    @Test
    void testDeliveredRule() {
        DeliveredRule rule = new DeliveredRule(wooService);
        PedidoDTO pedido = new PedidoDTO();
        pedido.setId(123);

        assertTrue(rule.evalua(pedido, TrackingStatusCategory.DELIVERED, 1));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.UNKNOWN, 1));

        rule.ejecuta(pedido, new CorreosInfo("Entregado", LocalDate.now()), 1);
        verify(wooService, times(1)).actualizarEstadoPedido(pedido, "completed");
    }

    @Test
    void testReturnedRule() {
        ReturnedRule rule = new ReturnedRule(wooService);
        PedidoDTO pedido = new PedidoDTO();
        pedido.setId(123);

        assertTrue(rule.evalua(pedido, TrackingStatusCategory.RETURNED, 1));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.DELIVERED, 1));

        rule.ejecuta(pedido, new CorreosInfo("Devuelto", LocalDate.now()), 1);
        verify(wooService, times(1)).actualizarEstadoPedido(pedido, "returned-cocex");
    }

    @Test
    void testReadyForPickupRule() {
        ReadyForPickupRule rule = new ReadyForPickupRule(telegramService);
        PedidoDTO pedido = new PedidoDTO();

        assertTrue(rule.evalua(pedido, TrackingStatusCategory.READY_FOR_PICKUP, 1));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.STUCK, 1));

        rule.ejecuta(pedido, new CorreosInfo("En oficina", LocalDate.now()), 1);
        verify(telegramService, times(1)).notificarPedidoDisposicion(pedido);
    }

    @Test
    void testStuckRule() {
        StuckRule rule = new StuckRule(telegramService);
        PedidoDTO pedido = new PedidoDTO();

        assertTrue(rule.evalua(pedido, TrackingStatusCategory.STUCK, 1));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.DELIVERED, 1));

        rule.ejecuta(pedido, new CorreosInfo("Parado", LocalDate.now()), 1);
        verify(telegramService, times(1)).alertarPedidoEstacionado(pedido);
    }

    @Test
    void testPreRegisteredRule() {
        PreRegisteredRule rule = new PreRegisteredRule(telegramService);
        PedidoDTO pedido = new PedidoDTO();

        assertTrue(rule.evalua(pedido, TrackingStatusCategory.PRE_REGISTERED, 4));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.PRE_REGISTERED, 2));
        assertFalse(rule.evalua(pedido, TrackingStatusCategory.DELIVERED, 4));

        rule.ejecuta(pedido, new CorreosInfo("Prerregistrado", LocalDate.now()), 4);
        verify(telegramService, times(1)).alertarPedidoNoEnviado(pedido, "PRERREGISTRADO", 4);
    }

    @Test
    void testLimpiarTelefono() {
        TelegramServiceImpl service = new TelegramServiceImpl(mock(RestClient.Builder.class), null);

        assertEquals("34666555444", service.limpiarTelefono("666 555 444"));
        assertEquals("34666555444", service.limpiarTelefono("+34 666 555 444"));
        assertEquals("34666555444", service.limpiarTelefono("34666555444"));
        assertEquals("34666555444", service.limpiarTelefono(" +34  666 555 444 "));
        assertNull(service.limpiarTelefono(null));
    }
}
