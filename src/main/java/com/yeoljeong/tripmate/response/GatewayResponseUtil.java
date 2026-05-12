package com.yeoljeong.tripmate.response;

import com.yeoljeong.tripmate.error.GatewayErrorCode;
import java.nio.charset.StandardCharsets;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class GatewayResponseUtil {

    public static Mono<Void> writeErrorResponse(ServerWebExchange exchange, GatewayErrorCode errorCode) {
        exchange.getResponse().setStatusCode(errorCode.getStatus());
        byte[] bytes = errorCode.getMessage().getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(bytes))
        );
    }
}
