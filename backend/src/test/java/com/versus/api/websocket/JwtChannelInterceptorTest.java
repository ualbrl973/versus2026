package com.versus.api.websocket;

import com.versus.api.auth.JwtService;
import com.versus.api.users.Role;
import com.versus.api.users.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("JwtChannelInterceptor")
class JwtChannelInterceptorTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-test-secret-test-secret-test-secret-aaaa";
    private static final long ACCESS_EXPIRY = 900L;
    private static final long REFRESH_EXPIRY = 604800L;
    private static final UUID USER_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    private final JwtService jwtService = new JwtService(SECRET, ACCESS_EXPIRY, REFRESH_EXPIRY);
    private final JwtChannelInterceptor interceptor = new JwtChannelInterceptor(jwtService);
    private final MessageChannel channelStub = mock(MessageChannel.class);

    private User testUser() {
        return User.builder()
                .id(USER_ID).username("wsplayer").email("ws@versus.com")
                .passwordHash("$2a$hash").role(Role.PLAYER).isActive(true)
                .build();
    }

    private Message<byte[]> connectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authHeader != null) accessor.addNativeHeader("Authorization", authHeader);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @DisplayName("Frame CONNECT")
    @Nested
    class Connect {

        @DisplayName("Sin header Authorization lanza MessageDeliveryException")
        @Test
        void sinHeaderAuth_lanza() {
            assertThatThrownBy(() -> interceptor.preSend(connectMessage(null), channelStub))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Missing Authorization");
        }

        @DisplayName("Header sin prefijo 'Bearer ' lanza MessageDeliveryException")
        @Test
        void sinPrefijoBearer_lanza() {
            assertThatThrownBy(() -> interceptor.preSend(connectMessage("Token foo"), channelStub))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Invalid Authorization");
        }

        @DisplayName("Token JWT inválido lanza MessageDeliveryException")
        @Test
        void tokenInvalido_lanza() {
            assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer no.es.jwt"), channelStub))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Invalid JWT");
        }

        @DisplayName("Token de refresh (type!=access) lanza MessageDeliveryException")
        @Test
        void tokenDeRefresh_lanza() {
            String refresh = jwtService.generateRefreshToken(testUser());
            assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer " + refresh), channelStub))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Not an access token");
        }

        @DisplayName("Token expirado lanza MessageDeliveryException")
        @Test
        void tokenExpirado_lanza() {
            JwtService expired = new JwtService(SECRET, -10L, REFRESH_EXPIRY);
            String token = expired.generateAccessToken(testUser());
            assertThatThrownBy(() -> interceptor.preSend(connectMessage("Bearer " + token), channelStub))
                    .isInstanceOf(MessageDeliveryException.class)
                    .hasMessageContaining("Invalid JWT");
        }

        @DisplayName("Token válido inyecta Authentication con userId como principal")
        @Test
        void tokenValido_inyectaAuthentication() {
            String token = jwtService.generateAccessToken(testUser());
            Message<byte[]> msg = connectMessage("Bearer " + token);

            Message<?> result = interceptor.preSend(msg, channelStub);

            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertThat(accessor.getUser())
                    .isInstanceOf(UsernamePasswordAuthenticationToken.class);
            assertThat(accessor.getUser().getName()).isEqualTo(USER_ID.toString());
        }

        @DisplayName("Token válido carga el authority ROLE_<role>")
        @Test
        void tokenValido_cargaAuthorityRole() {
            String token = jwtService.generateAccessToken(testUser());
            Message<byte[]> msg = connectMessage("Bearer " + token);

            Message<?> result = interceptor.preSend(msg, channelStub);

            UsernamePasswordAuthenticationToken auth =
                    (UsernamePasswordAuthenticationToken) StompHeaderAccessor.wrap(result).getUser();
            assertThat(auth.getAuthorities())
                    .extracting(Object::toString)
                    .containsExactly("ROLE_PLAYER");
        }
    }

    @DisplayName("Otros frames")
    @Nested
    class OtrosFrames {

        @DisplayName("Frame SEND no es validado (auth ya hecha en CONNECT)")
        @Test
        void frameSend_pasaSinValidacion() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
            accessor.setDestination("/app/whatever");
            Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatCode(() -> interceptor.preSend(msg, channelStub))
                    .doesNotThrowAnyException();
        }

        @DisplayName("Frame SUBSCRIBE no es validado")
        @Test
        void frameSubscribe_pasaSinValidacion() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            accessor.setDestination("/topic/foo");
            Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatCode(() -> interceptor.preSend(msg, channelStub))
                    .doesNotThrowAnyException();
        }

        @DisplayName("Frame DISCONNECT no es validado")
        @Test
        void frameDisconnect_pasaSinValidacion() {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
            Message<byte[]> msg = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

            assertThatCode(() -> interceptor.preSend(msg, channelStub))
                    .doesNotThrowAnyException();
        }
    }
}
