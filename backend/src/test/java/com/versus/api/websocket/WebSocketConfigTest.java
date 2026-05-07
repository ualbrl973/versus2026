package com.versus.api.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketConfig — Spring context wiring")
@SpringBootTest
class WebSocketConfigTest {

    @Autowired
    ApplicationContext ctx;

    @DisplayName("WebSocketConfig se carga como bean")
    @Test
    void webSocketConfig_estaRegistrado() {
        assertThat(ctx.getBean(WebSocketConfig.class)).isNotNull();
    }

    @DisplayName("JwtChannelInterceptor se carga como bean")
    @Test
    void jwtChannelInterceptor_estaRegistrado() {
        assertThat(ctx.getBean(JwtChannelInterceptor.class)).isNotNull();
    }

    @DisplayName("EchoController se carga como bean (smoke endpoint)")
    @Test
    void echoController_estaRegistrado() {
        assertThat(ctx.getBean(EchoController.class)).isNotNull();
    }

    @DisplayName("SimpMessagingTemplate está disponible para enviar a /topic y /user")
    @Test
    void simpMessagingTemplate_estaDisponible() {
        // Indica que @EnableWebSocketMessageBroker hizo su trabajo y el broker está configurado.
        assertThat(ctx.getBean(SimpMessagingTemplate.class)).isNotNull();
    }
}
