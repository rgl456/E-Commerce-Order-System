package com.ragul.OrderService.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InternalRequestFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER    = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String roles = request.getHeader(USER_ROLES_HEADER);

        if(userId != null && !userId.isEmpty()){
            List<SimpleGrantedAuthority> authorities = List.of();

            if(roles != null && !roles.isEmpty()){
                authorities = Arrays.stream(roles.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(role -> new SimpleGrantedAuthority(
                                role.startsWith("ROLE_") ? role : "ROLE_" + role
                        ))
                        .toList();
            }

            var authentication = new UsernamePasswordAuthenticationToken(
              userId,
              false,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Set security context for user: {} with roles: {}", userId, roles);

        }

        filterChain.doFilter(request, response);
    }
}
