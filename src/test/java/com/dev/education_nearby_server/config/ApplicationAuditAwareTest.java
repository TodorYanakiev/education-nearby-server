package com.dev.education_nearby_server.config;

import com.dev.education_nearby_server.models.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationAuditAwareTest {

    private final ApplicationAuditAware auditAware = new ApplicationAuditAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditorReturnsEmptyWhenPrincipalIsNotApplicationUser() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("oidc-user", null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(auditAware.getCurrentAuditor()).isEmpty();
    }

    @Test
    void getCurrentAuditorReturnsUserIdWhenPrincipalIsApplicationUser() {
        User user = User.builder().id(42L).build();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(user, null, "ROLE_USER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(auditAware.getCurrentAuditor()).contains(42L);
    }

    @Test
    void getCurrentAuditorReturnsEmptyForAnonymousAuthentication() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(auditAware.getCurrentAuditor()).isEmpty();
    }
}
