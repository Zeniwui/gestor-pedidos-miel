# Motor de automatización logística y bot de notificaciones

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)

Una herramienta de automatización construida con Spring Boot diseñada para sincronizar y gestionar el estado de los envíos de una tienda de miel en WooCommerce. El sistema extrae la información de seguimiento directamente desde Correos, actualiza la tienda online y envía notificaciones interactivas por Telegram para agilizar la atención al cliente vía WhatsApp.

## ¿Por qué existe este proyecto?

El plugin de Correos integrado en nuestra página web Wordpress presentaba problemas de sincronización y no actualizaba el estado de los pedidos en WooCommerce. Este proyecto nace para:
1. **Automatizar el seguimiento:** Consultar el estado real de los paquetes haciendo web scraping en la web de Correos.
2. **Mantener WooCommerce al día:** Actualizar automáticamente los estados (ej. `completed`, `returned-cocex`) basándose en la información de Correos.
3. **Mejorar la Atención al Cliente:** Enviar mensajes por Telegram con enlaces directos (`wa.me`) listos para notificar al cliente por WhatsApp con un solo clic.
