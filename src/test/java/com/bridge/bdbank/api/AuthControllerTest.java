package com.bridge.bdbank.api;

import com.bridge.bdbank.api.dto.LoginRequest;
import com.bridge.bdbank.auth.AuthenticationException;
import com.bridge.bdbank.auth.AuthenticationService;
import com.bridge.bdbank.persistence.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    @Test
    void devraitRetournerUneSessionApresConnexion() throws Exception {
        User user = User.builder().id(1L).username("admin").role("ADMIN").build();
        when(authenticationService.login("admin", "MotDePasse123456")).thenReturn("token-test");
        when(authenticationService.getUserInfo("token-test")).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "MotDePasse123456"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token-test"))
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void devraitRefuserUneConnexionInvalide() throws Exception {
        when(authenticationService.login("admin", "mauvais-mot-de-passe"))
            .thenThrow(new AuthenticationException("Identifiants invalides"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "mauvais-mot-de-passe"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    @Test
    void devraitRetournerLeProfilAvecUnToken() throws Exception {
        User user = User.builder().id(1L).username("admin").role("ADMIN").build();
        when(authenticationService.getUserInfo("token-test")).thenReturn(user);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer token-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void devraitRefuserLeProfilSansToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Token manquant"));
    }

    @Test
    void devraitDeconnecterUneSession() throws Exception {
        doNothing().when(authenticationService).logout("token-test");

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer token-test"))
            .andExpect(status().isNoContent());

        verify(authenticationService).logout("token-test");
    }

    @Test
    void devraitRefuserSetupAvecUneMauvaiseCle() throws Exception {
        mockMvc.perform(post("/auth/setup")
                .header("X-Setup-Key", "wrong-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin", "MotDePasse123456"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Initialisation non autorisée"));
    }
}
