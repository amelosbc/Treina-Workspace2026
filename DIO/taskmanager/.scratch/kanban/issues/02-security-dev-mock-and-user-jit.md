# 02: Estrutura de Segurança, Dev Mock e Sincronização JIT de Usuário

**What to build:**
Configurar a camada de segurança com Spring Security permitindo que requisições com o cabeçalho `X-Mock-User` (no perfil `dev`) autentiquem o usuário sem precisar de chamadas externas ao Clerk, sincronizando e persistindo a entidade `User` local automaticamente (*Just-in-Time*).

**Blocked by:** 01: Persistência H2 em Arquivo e Tipos de Domínio

**Status:** done

- [x] Adicionar dependência `spring-boot-starter-security` e OAuth2 Resource Server no `pom.xml`.
- [x] Criar entidade `User` (`id`, `clerkUserId`, `email`, `name`, timestamps) e `UserRepository`.
- [x] Implementar `DevMockAuthFilter` e `SecurityConfig` liberando Swagger UI (`/swagger-ui/**`, `/api-docs/**`) e H2 Console (`/h2-console/**`).
- [x] Implementar serviço de sincronização JIT (`UserService`) que localiza ou cadastra o usuário no banco local.
- [x] Testes de integração validando a autenticação via mock e criação automática de usuário.
