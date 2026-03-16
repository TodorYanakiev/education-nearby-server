package com.dev.education_nearby_server.config;

import com.dev.education_nearby_server.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2BeansConfigurationTest {

    @Test
    void httpCookieOAuth2AuthorizationRequestRepositoryCreatesBean() {
        OAuth2BeansConfiguration configuration = new OAuth2BeansConfiguration();

        HttpCookieOAuth2AuthorizationRequestRepository repository =
                configuration.httpCookieOAuth2AuthorizationRequestRepository();

        assertThat(repository).isNotNull();
    }
}
