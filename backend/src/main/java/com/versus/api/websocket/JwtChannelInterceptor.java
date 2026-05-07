package com.versus.api.websocket;

import com.versus.api.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            throw new MessageDeliveryException("Missing Authorization header");
        }
        String header = authHeaders.get(0);
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessageDeliveryException("Invalid Authorization header");
        }
        String token = header.substring(7);

        try {
            Claims claims = jwtService.parse(token);
            if (!"access".equals(claims.get("type", String.class))) {
                throw new MessageDeliveryException("Not an access token");
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String role = claims.get("role", String.class);
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            accessor.setUser(auth);
        } catch (JwtException | IllegalArgumentException e) {
            MessageDeliveryException ex = new MessageDeliveryException("Invalid JWT");
            ex.initCause(e);
            throw ex;
        }
        return message;
    }
}
