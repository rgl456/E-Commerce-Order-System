package com.ragul.ApiGateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UserContextFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> {
                    Jwt jwt = (Jwt) auth.getPrincipal();
                    String userId = jwt.getSubject();
                    String email = jwt.getClaimAsString("email");

                    String roles = extractRole(jwt);

                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId != null ? userId : "")
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Roles", roles)

                            // Strip the original Authorization header
                            // Downstream services don't need the raw JWT
                            // They trust the X-User-* headers instead
                            .headers(header -> header.remove("Authorization"))
                            .build();

                    return exchange.mutate()
                            .request(mutatedRequest)
                            .build();
                })
                .defaultIfEmpty(exchange)
                .flatMap(ex -> chain.filter(ex));

    }

    @SuppressWarnings("unchecked")
    private String extractRole(Jwt jwt) {
        try{
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if(realmAccess == null) return "";

            List<String> roles = (List<String>) realmAccess.get("roles");
            if(roles == null) return "";

            return String.join(",", roles);
        }
        catch (Exception e){
            log.warn("Could not extract roles from JWT: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 50;
    }

}
