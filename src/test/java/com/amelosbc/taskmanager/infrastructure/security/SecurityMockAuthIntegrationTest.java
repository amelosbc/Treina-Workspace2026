package com.amelosbc.taskmanager.infrastructure.security;

import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityMockAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized quando endpoint protegido for acessado sem autenticação")
    void shouldReturn401WhenAccessingProtectedEndpointWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Deve permitir acesso público à documentação Swagger UI e OpenAPI docs")
    void shouldAllowPublicAccessToSwaggerAndOpenApiDocs() throws Exception {
        // Swagger UI (redirecionamento ou index)
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        // OpenAPI JSON Docs
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve permitir acesso público ao H2 Console sem bloquear com 401 ou 403")
    void shouldAllowPublicAccessToH2Console() throws Exception {
        // O H2 console é um servlet registrado fora do MockMvc DispatcherServlet, então o Spring Security não deve retornar 401 Unauthorized
        mockMvc.perform(get("/h2-console"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
    }

    @Test
    @DisplayName("Deve autenticar com sucesso via X-Mock-User e sincronizar usuário JIT no banco")
    void shouldAuthenticateAndSyncUserJitWithMockHeader() throws Exception {
        String mockClerkId = "user_clerk_dev_999";

        // Antes da requisição, o usuário não existe no banco
        assertThat(userRepository.findByClerkUserId(mockClerkId)).isEmpty();

        // Faz requisição autenticada com X-Mock-User
        mockMvc.perform(get("/api/tasks")
                .header("X-Mock-User", mockClerkId))
                .andExpect(status().isOk());

        // Valida que o usuário foi criado automaticamente no banco (JIT)
        Optional<User> createdUser = userRepository.findByClerkUserId(mockClerkId);
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getEmail()).isEqualTo(mockClerkId + "@taskmanager.local");
        assertThat(createdUser.get().getName()).contains(mockClerkId);
    }

    @Test
    @DisplayName("Deve reutilizar usuário existente em requisições subsequentes sem duplicar registro")
    void shouldReuseExistingUserWithoutDuplication() throws Exception {
        String mockClerkId = "user_clerk_existing";

        // Primeira requisição cria o usuário
        mockMvc.perform(get("/api/tasks").header("X-Mock-User", mockClerkId))
                .andExpect(status().isOk());

        long initialCount = userRepository.count();
        assertThat(initialCount).isEqualTo(1);

        // Segunda requisição reutiliza o mesmo usuário
        mockMvc.perform(get("/api/tasks").header("X-Mock-User", mockClerkId))
                .andExpect(status().isOk());

        assertThat(userRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Deve isolar múltiplos usuários mock criando registros distintos")
    void shouldSupportMultipleIndependentMockUsers() throws Exception {
        mockMvc.perform(get("/api/tasks").header("X-Mock-User", "user_alice"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tasks").header("X-Mock-User", "user_bob"))
                .andExpect(status().isOk());

        assertThat(userRepository.count()).isEqualTo(2);
        assertThat(userRepository.findByClerkUserId("user_alice")).isPresent();
        assertThat(userRepository.findByClerkUserId("user_bob")).isPresent();
    }
}
