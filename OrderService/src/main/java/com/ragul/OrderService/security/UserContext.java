package com.ragul.OrderService.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

// @RequestScope means Spring creates ONE instance of this per HTTP request
// Each request gets its own UserContext with that request's user's data
// thread safe
@Component
@RequestScope
public class UserContext {

    private Jwt jwt;

    public UserContext(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() instanceof Jwt){
            this.jwt = (Jwt) authentication.getPrincipal();
        }
        else{
            this.jwt = null;
        }
    }

    public String getUserId(){
        return jwt != null ? jwt.getSubject() : null;
    }

    public String getEmail() {
        return jwt != null
                ? jwt.getClaimAsString("email")
                : null;
    }

    public boolean isAdmin() {
        return getRoles().contains("ROLE_ADMIN");
    }

    public boolean isCustomer() {
        return getRoles().contains("ROLE_CUSTOMER");
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles() {
        if (jwt == null) return List.of();

        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return List.of();

        var roles = (List<String>) realmAccess.get("roles");
        return roles != null ? roles : List.of();
    }

}
