package com.versus.api.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

// TODO(#90): remove once the lobby/match flow exercises the WebSocket end-to-end.
@Controller
public class EchoController {

    @MessageMapping("/ping")
    @SendToUser("/queue/ping")
    public Map<String, Object> ping(Principal principal, @Payload(required = false) String payload) {
        return Map.of(
                "userId", principal.getName(),
                "echo", payload == null ? "ping" : payload,
                "timestamp", Instant.now().toString());
    }
}
