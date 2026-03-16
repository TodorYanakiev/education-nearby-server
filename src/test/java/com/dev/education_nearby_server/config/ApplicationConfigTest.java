package com.dev.education_nearby_server.config;

import com.dev.education_nearby_server.enums.Role;
import com.dev.education_nearby_server.models.entity.User;
import com.dev.education_nearby_server.repositories.TokenRepository;
import com.dev.education_nearby_server.repositories.UserRepository;
import com.dev.education_nearby_server.services.LogoutService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    private ApplicationConfig config;

    @BeforeEach
    void setUp() {
        config = new ApplicationConfig(userRepository);
    }

    @Test
    void modelMapperCreatesInstance() {
        assertThat(config.modelMapper()).isNotNull();
    }

    @Test
    void objectMapperRegistersJavaTimeAndDisablesTimestampSerialization() {
        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper).isNotNull();
        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void userDetailsServiceFindsByEmailFirst() {
        User user = User.builder()
                .email("john@example.com")
                .username("johnny")
                .role(Role.USER)
                .enabled(true)
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(java.util.Optional.of(user));

        UserDetailsService service = config.userDetailsService();
        User result = (User) service.loadUserByUsername("john@example.com");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void userDetailsServiceFallsBackToUsernameLookup() {
        User user = User.builder()
                .email("john@example.com")
                .username("johnny")
                .role(Role.USER)
                .enabled(true)
                .build();
        when(userRepository.findByEmail("johnny")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUsername("johnny")).thenReturn(java.util.Optional.of(user));

        UserDetailsService service = config.userDetailsService();
        User result = (User) service.loadUserByUsername("johnny");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void userDetailsServiceThrowsWhenUserMissing() {
        when(userRepository.findByEmail("missing")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUsername("missing")).thenReturn(java.util.Optional.empty());

        UserDetailsService service = config.userDetailsService();

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing"));
    }

    @Test
    void authenticationProviderUsesDaoAuthenticationProvider() {
        AuthenticationProvider provider = config.authenticationProvider();

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    void auditorAwareReturnsApplicationAuditAware() {
        assertThat(config.auditorAware()).isInstanceOf(ApplicationAuditAware.class);
    }

    @Test
    void logoutHandlerCreatesLogoutService() {
        assertThat(config.logoutHandler(tokenRepository)).isInstanceOf(LogoutService.class);
    }

    @Test
    void passwordEncoderCreatesBcryptEncoder() {
        PasswordEncoder encoder = config.passwordEncoder();

        assertThat(encoder.matches("pass1234", encoder.encode("pass1234"))).isTrue();
    }

    @Test
    void javaMailSenderAppliesProvidedProperties() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.port", "587")
                .withProperty("spring.mail.username", "mailer")
                .withProperty("spring.mail.password", "secret")
                .withProperty("spring.mail.properties.mail.transport.protocol", "smtp")
                .withProperty("spring.mail.properties.mail.smtp.auth", "true")
                .withProperty("spring.mail.properties.mail.smtp.starttls.enable", "true")
                .withProperty("spring.mail.properties.mail.debug", "false");

        JavaMailSenderImpl sender = (JavaMailSenderImpl) config.javaMailSender(env);

        assertThat(sender.getHost()).isEqualTo("smtp.example.com");
        assertThat(sender.getPort()).isEqualTo(587);
        assertThat(sender.getUsername()).isEqualTo("mailer");
        assertThat(sender.getPassword()).isEqualTo("secret");
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
    }

    @Test
    void javaMailSenderLeavesOptionalFieldsUnsetWhenBlank() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.mail.host", " ")
                .withProperty("spring.mail.username", " ")
                .withProperty("spring.mail.password", " ");

        JavaMailSenderImpl sender = (JavaMailSenderImpl) config.javaMailSender(env);

        assertThat(sender.getHost()).isNull();
        assertThat(sender.getUsername()).isNull();
        assertThat(sender.getPassword()).isNull();
        assertThat(sender.getJavaMailProperties().getProperty("mail.transport.protocol")).isEqualTo("smtp");
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(sender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(sender.getJavaMailProperties().getProperty("mail.debug")).isEqualTo("false");
    }
}
