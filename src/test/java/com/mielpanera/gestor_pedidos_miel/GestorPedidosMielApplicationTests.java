package com.mielpanera.gestor_pedidos_miel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"STORE_URL=https://dummy.example.com",
		"WOO_CONSUMER_KEY=dummy_key",
		"WOO_CONSUMER_SECRET=dummy_secret",
		"TELEGRAM_TOKEN=dummy_token",
		"TELEGRAM_CHAT_ID=dummy_chat_id",
		"TELEFONO_OFICIAL=dummy_phone"
})
class GestorPedidosMielApplicationTests {

	@Test
	void contextLoads() {
	}

}
