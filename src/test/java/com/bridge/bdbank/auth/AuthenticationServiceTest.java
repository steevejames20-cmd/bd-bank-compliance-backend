package com.bridge.bdbank.auth;

import com.bridge.bdbank.persistence.User;
import com.bridge.bdbank.persistence.UserRepository;
import com.bridge.bdbank.persistence.UserSession;
import com.bridge.bdbank.persistence.UserSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository sessionRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("admin")
            .passwordHash(passwordEncoder.encode("password123"))
            .role("ADMIN")
            .active(true)
            .failedLoginAttempts(0)
            .build();
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String token = authenticationService.login("admin", "password123");

        assertThat(token).isNotNull();
        assertThat(token).hasSizeGreaterThan(20); // Base64 encoded token
        verify(sessionRepository).save(any(UserSession.class));
    }

    @Test
    void shouldFailLoginWithInvalidPassword() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.login("admin", "wrongpassword"))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Identifiants invalides");

        verify(userRepository).save(testUser); // Failed attempt should be recorded
    }

    @Test
    void shouldFailLoginWithNonExistentUser() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.login("nonexistent", "password"))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("Identifiants invalides");
    }

    @Test
    void shouldLockAccountAfterMaxFailedAttempts() {
        testUser.setFailedLoginAttempts(4); // One more attempt will lock
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.login("admin", "wrongpassword"))
            .isInstanceOf(AuthenticationException.class);

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(testUser.getLockedUntil()).isNotNull();
        assertThat(testUser.getLockedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void shouldPreventLoginWhenAccountLocked() {
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authenticationService.login("admin", "password123"))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("verrouillé");
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        testUser.setFailedLoginAttempts(3);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authenticationService.login("admin", "password123");

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(testUser.getLockedUntil()).isNull();
    }

    @Test
    void shouldLogoutSuccessfully() {
        UserSession session = UserSession.builder()
            .id(1L)
            .token("test-token")
            .user(testUser)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .lastActivityAt(LocalDateTime.now())
            .build();

        when(sessionRepository.findByToken("test-token")).thenReturn(Optional.of(session));

        authenticationService.logout("test-token");

        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldValidateTokenSuccessfully() {
        UserSession session = UserSession.builder()
            .id(1L)
            .token("valid-token")
            .user(testUser)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .lastActivityAt(LocalDateTime.now())
            .build();

        when(sessionRepository.findByToken("valid-token")).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = authenticationService.validateToken("valid-token");

        assertThat(user).isEqualTo(testUser);
        verify(sessionRepository).save(session); // Last activity should be updated
    }

    @Test
    void shouldFailTokenValidationWithExpiredSession() {
        UserSession session = UserSession.builder()
            .id(1L)
            .token("expired-token")
            .user(testUser)
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .lastActivityAt(LocalDateTime.now().minusMinutes(1))
            .build();

        when(sessionRepository.findByToken("expired-token")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authenticationService.validateToken("expired-token"))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("expirée");

        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldFailTokenValidationWithInactiveSession() {
        UserSession session = UserSession.builder()
            .id(1L)
            .token("inactive-token")
            .user(testUser)
            .expiresAt(LocalDateTime.now().plusMinutes(30))
            .lastActivityAt(LocalDateTime.now().minusMinutes(20))
            .build();

        when(sessionRepository.findByToken("inactive-token")).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authenticationService.validateToken("inactive-token"))
            .isInstanceOf(AuthenticationException.class)
            .hasMessageContaining("inactive");

        verify(sessionRepository).delete(session);
    }

    @Test
    void shouldCleanupExpiredSessions() {
        authenticationService.cleanupExpiredSessions();

        verify(sessionRepository).deleteExpiredSessions(any(LocalDateTime.class));
        verify(sessionRepository).deleteInactiveSessions(any(LocalDateTime.class));
    }
}