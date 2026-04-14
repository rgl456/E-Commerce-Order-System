package com.ragul.OrderService.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2FeignInterceptor implements RequestInterceptor {

    private final OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager ;

    @Override
    public void apply(RequestTemplate template) {
        try{
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("order-service-client")
                    .principal("order-service-client")
                    .build();

            var authorizedClient = oAuth2AuthorizedClientManager.authorize(authorizeRequest);

            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String token = authorizedClient.getAccessToken().getTokenValue();

                template.header("Authorization", "Bearer " + token);

                log.debug("Service token attached to outgoing {} request to {}",
                        template.method(),
                        template.url());
            }
            else{
                log.error("Could not obtain service token — " + "Feign call will proceed without authorization");
            }
        }
        catch(Exception e){
            log.error("Failed to obtain service token: {}", e.getMessage());
        }
    }

}
