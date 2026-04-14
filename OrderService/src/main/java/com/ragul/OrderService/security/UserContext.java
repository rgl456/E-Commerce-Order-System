package com.ragul.OrderService.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Arrays;
import java.util.List;

// @RequestScope means Spring creates ONE instance of this per HTTP request
// Each request gets its own UserContext with that request's user's data
// thread safe
@Component
@RequestScope
@Slf4j
public class UserContext {

    private static final String USER_ID_HEADER    = "X-User-Id";
    private static final String USER_EMAIL_HEADER = "X-User-Email";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private final String userId;
    private final String userEmail;
    private final List<String> roles;

    public UserContext(HttpServletRequest request){
        this.userId = getHeader(request, USER_ID_HEADER);
        this.userEmail = getHeader(request, USER_EMAIL_HEADER);
        String rolesHeader = getHeader(request, USER_ROLES_HEADER);

        this.roles = rolesHeader.isEmpty() ? List.of() : Arrays.asList(rolesHeader.split(","));
    }

    private String getHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value != null ? value.trim() : "";
    }

    public String getUserId(){
        return userId;
    }

    public String getEmail() {
        return userEmail;
    }

    public List<String> getRoles(){
        return roles;
    }

    public boolean isAdmin() {
        return roles.contains("ROLE_ADMIN");
    }

    public boolean isCustomer() {
        return roles.contains("ROLE_CUSTOMER");
    }

    public boolean isCurrentUser(String id) {
        return userId != null && userId.equals(id);
    }

}
