package tech.nocountry.onboarding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tech.nocountry.onboarding.dto.AuthResponse;
import tech.nocountry.onboarding.dto.LoginRequest;
import tech.nocountry.onboarding.services.AuthService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerAndLogin_JuanPerez_success() throws Exception {
        // Datos de Juan Pérez
        Map<String, String> registerPayload = new HashMap<>();
        registerPayload.put("fullName", "Juan Pérez");
        registerPayload.put("username", "juanperez");
        registerPayload.put("email", "juan@example.com");
        registerPayload.put("password", "Pass@123");
        registerPayload.put("phone", "+1234567890");

        // Stub de registro
        Map<String, Object> registerResponse = new HashMap<>();
        registerResponse.put("success", true);
        registerResponse.put("message", "Usuario registrado");
        registerResponse.put("email", "juan@example.com");
        when(authService.registerUser(any(), any(), any(), any(), any(), any())).thenReturn(registerResponse);

        // Ejecutar registro
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("juan@example.com"));

        // Stub de login simple
        AuthResponse loginOk = AuthResponse.builder()
                .success(true)
                .message("Login exitoso")
                .email("juan@example.com")
                .username("juanperez")
                .fullName("Juan Pérez")
                .userId("user-juan-id")
                .token("jwt-token-mock")
                .build();
        when(authService.loginUserSimple(any(LoginRequest.class))).thenReturn(loginOk);

        // Ejecutar login
        Map<String, String> loginPayload = new HashMap<>();
        loginPayload.put("email", "juan@example.com");
        loginPayload.put("password", "Pass@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("juan@example.com"))
                .andExpect(jsonPath("$.username").value("juanperez"))
                .andExpect(jsonPath("$.fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.token").exists());
    }
}
