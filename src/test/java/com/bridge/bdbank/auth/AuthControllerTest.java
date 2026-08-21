package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthController authController;

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("admin", "password123");
        User user = User.builder()
            .id(1L)
            .username("admin")
            .role("ADMIN")
            .build();

        when(authenticationService.login("admin", "password123")).thenReturn("valid-token-123");
        when(authenticationService.getUserInfo("valid-token-123")).thenReturn(user);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("valid-token-123");
        assertThat(response.getBody().username()).isEqualTo("admin");
        assertThat(response.getBody().role()).isEqualTo("ADMIN");
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() {
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        when(authenticationService.login("admin", "wrongpassword"))
            .thenThrow(new AuthenticationException("Identifiants invalides"));

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldLogoutSuccessfully() {
        ResponseEntity<Void> response = authController.logout("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldLogoutWithNullToken() {
        ResponseEntity<Void> response = authController.logout(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldGetCurrentUserSuccessfully() {
        User user = User.builder()
            .id(1L)
            .username("admin")
            .role("ADMIN")
            .build();

        when(authenticationService.getUserInfo("valid-token")).thenReturn(user);

        ResponseEntity<User> response = authController.getCurrentUser("Bearer valid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo("admin");
        assertThat(response.getBody().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void shouldFailGetCurrentUserWithInvalidToken() {
        when(authenticationService.getUserInfo("invalid-token"))
            .thenThrow(new AuthenticationException("Token invalide"));

        ResponseEntity<User> response = authController.getCurrentUser("Bearer invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailGetCurrentUserWithoutToken() {
        ResponseEntity<User> response = authController.getCurrentUser(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailGetCurrentUserWithMalformedToken() {
        ResponseEntity<User> response = authController.getCurrentUser("InvalidFormat");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}