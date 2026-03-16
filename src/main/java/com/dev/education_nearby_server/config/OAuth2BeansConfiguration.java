package com.dev.education_nearby_server.config;

import com.dev.education_nearby_server.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OAuth2-related bean definitions kept separate from the security filter chain configuration
 * to avoid bean initialization cycles.
 */
@Configuration
public class OAuth2BeansConfiguration {

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }
}
