# Notas de Design e Especificação Técnica - Ticket 02
## Estrutura de Segurança, Dev Mock e Sincronização JIT de Usuário

**Referência do Ticket:** `.scratch/kanban/issues/02-security-dev-mock-and-user-jit.md`  
**Documentos Base:** `SPEC.md`, `DESIGN.md` (Seção 3), `docs/adr/0001-clerk-auth-and-dev-mock.md`

---

## 1. Dependências do Maven (`pom.xml`)

Para suportar Spring Security, validação de tokens JWT (Resource Server Clerk) e testes de segurança com MockMvc, devem ser adicionadas as seguintes dependências no `pom.xml`:

```xml
        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- OAuth2 Resource Server (Validação JWT Clerk em Produção) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <!-- Spring Security Test (MockMvc security helpers) -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```

> **Atenção Técnica Crítica:**  
> A inclusão de `spring-boot-starter-oauth2-resource-server` ativa a autoconfiguração do Spring Boot que busca por `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` ou `issuer-uri`. Em ambiente local/dev/test, sem essa propriedade preenchida, o Spring Boot falhará ao inicializar o contexto.  
> **Solução Recomendada:**  
> Condicionar o `oauth2ResourceServer` na `SecurityConfig` para ser ativado apenas se a propriedade existir ou quando o perfil for diferente de `dev` e `test`, ou prover uma configuração dummy em `application.properties` (ex: `dev` usando autenticação mock e `prod` exigindo o JWKS do Clerk).

---

## 2. Entidade de Domínio `User` e Repositório

### 2.1. Entidade JPA (`com.amelosbc.taskmanager.domain.User`)
```java
package com.amelosbc.taskmanager.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clerk_user_id", nullable = false, unique = true, length = 120)
    private String clerkUserId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public User() {}

    public User(String clerkUserId, String email, String name) {
        this.clerkUserId = clerkUserId;
        this.email = email;
        this.name = name;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClerkUserId() { return clerkUserId; }
    public void setClerkUserId(String clerkUserId) { this.clerkUserId = clerkUserId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

### 2.2. Repositório (`com.amelosbc.taskmanager.domain.UserRepository`)
```java
package com.amelosbc.taskmanager.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByClerkUserId(String clerkUserId);
}
```

---

## 3. Contrato do Contexto do Usuário Autenticado (`CurrentUserContext`)

Para isolamento de arquitetura limpa, as camadas de aplicação e controllers não devem depender de tokens brutos ou do Spring Security diretamente. O contexto autenticado é exposto via um Record imutável.

### 3.1. Record DTO (`com.amelosbc.taskmanager.application.dto.CurrentUserContext`)
```java
package com.amelosbc.taskmanager.application.dto;

public record CurrentUserContext(
    Long userId,          // ID interno no banco (PK local)
    String clerkUserId,   // ID externo (sub do Clerk ou X-Mock-User)
    String email,         // E-mail do usuário
    String name           // Nome do usuário
) {
    public static CurrentUserContext fromDomain(com.amelosbc.taskmanager.domain.User user) {
        return new CurrentUserContext(
            user.getId(),
            user.getClerkUserId(),
            user.getEmail(),
            user.getName()
        );
    }
}
```

### 3.2. Mecanismo de Injeção nos Controllers / Services
O `SecurityContextHolder` armazena uma instância de `UsernamePasswordAuthenticationToken` cujo `principal` é o próprio `CurrentUserContext` (com role `ROLE_USER`).
Dessa forma, qualquer controller pode injetar diretamente:
```java
@GetMapping
public ResponseEntity<List<BoardDTO>> listBoards(
    @AuthenticationPrincipal CurrentUserContext currentUser
) {
    return ResponseEntity.ok(boardService.listByUser(currentUser.userId()));
}
```

---

## 4. Serviço de Sincronização JIT (`UserService`)

Conforme `SPEC.md` e `DESIGN.md`, no primeiro acesso do usuário (seja via Clerk JWT ou `X-Mock-User`), o backend deve sincronizar e persistir o registro local caso ainda não exista:

```java
package com.amelosbc.taskmanager.application;

import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import com.amelosbc.taskmanager.domain.User;
import com.amelosbc.taskmanager.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public CurrentUserContext syncAndGetUser(String clerkUserId, String email, String name) {
        User user = userRepository.findByClerkUserId(clerkUserId)
            .orElseGet(() -> {
                User newUser = new User(clerkUserId, email, name);
                return userRepository.save(newUser);
                // Nota: O Ticket 03 adicionará aqui a criação do "Meu Primeiro Quadro" e tarefas onboarding.
            });

        return CurrentUserContext.fromDomain(user);
    }
}
```

---

## 5. Filtro de Autenticação Dev Mock (`DevMockAuthFilter`)

O filtro deve rodar no perfil `dev` (e `test`), interceptando o cabeçalho `X-Mock-User`.

```java
package com.amelosbc.taskmanager.infrastructure.security;

import com.amelosbc.taskmanager.application.UserService;
import com.amelosbc.taskmanager.application.dto.CurrentUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Profile({"dev", "test"})
public class DevMockAuthFilter extends OncePerRequestFilter {

    public static final String MOCK_USER_HEADER = "X-Mock-User";
    private final UserService userService;

    public DevMockAuthFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String mockUserId = request.getHeader(MOCK_USER_HEADER);

        if (mockUserId != null && !mockUserId.trim().isEmpty()) {
            String cleanUserId = mockUserId.trim();
            String mockEmail = cleanUserId + "@taskmanager.local";
            String mockName = "Mock User (" + cleanUserId + ")";

            CurrentUserContext userContext = userService.syncAndGetUser(cleanUserId, mockEmail, mockName);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
            var auth = new UsernamePasswordAuthenticationToken(userContext, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## 6. Configuração do `SecurityFilterChain` e Liberação de Rotas Públicas

Requisitos críticos atendidos:
1. **Swagger UI e OpenAPI:** `/swagger-ui/**`, `/swagger-ui.html`, `/api-docs/**`, `/v3/api-docs/**` liberados sem autenticação.
2. **H2 Console:**
   - Rota `/h2-console/**` liberada sem autenticação.
   - CSRF desabilitado / ignorado para `/h2-console/**`.
   - `frameOptions` configurado como `sameOrigin` (o console H2 utiliza iframes na UI).
3. **Sessão Stateless:** `SessionCreationPolicy.STATELESS`.
4. **Proteção Geral:** Qualquer rota `/api/**` exige autenticação.

```java
package com.amelosbc.taskmanager.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DevMockAuthFilter devMockAuthFilter;

    public SecurityConfig(@Autowired(required = false) DevMockAuthFilter devMockAuthFilter) {
        this.devMockAuthFilter = devMockAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**")
                .disable() // API REST stateless
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // Necessário para iframes do H2 Console
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/h2-console/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        if (devMockAuthFilter != null) {
            http.addFilterBefore(devMockAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }
}
```

---

## 7. Especificação Exata dos Testes MockMvc

O teste de integração deve validar:
1. Rejeição de endpoints protegidos sem cabeçalho de autenticação (HTTP 401).
2. Acesso a endpoints públicos (Swagger e H2 Console) sem credenciais (HTTP 200 ou 302).
3. Autenticação bem-sucedida via cabeçalho `X-Mock-User`.
4. Sincronização e persistência JIT no banco de dados (criação automática do registro `User`).
5. Idempotência: requisições subsequentes com o mesmo `X-Mock-User` não duplicam registros na base.
6. Isolamento: múltiplos `X-Mock-User` criam usuários distintos e isolados.

### 7.1. Código do Teste de Integração (`SecurityMockAuthIntegrationTest.java`)

```java
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
    @DisplayName("Deve permitir acesso público ao H2 Console sem autenticação")
    void shouldAllowPublicAccessToH2Console() throws Exception {
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().is3xxRedirection()); // H2 console redireciona ou retorna 200
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
```

---

## 8. Checklist de Implementação para o Ticket 02

- [x] Especificação técnica e snippets de dependências validados.
- [x] Contrato `CurrentUserContext` isolando domínio e segurança.
- [x] Especificação de bypass/permissões para Swagger UI e H2 Console com frameOptions `sameOrigin`.
- [x] Especificação do filtro `DevMockAuthFilter` e estratégia JIT.
- [x] Suíte de testes MockMvc completa pronta para execução via TDD.
