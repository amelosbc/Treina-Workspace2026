# 03: Gerenciamento de Quadros (Boards) e Onboarding Automático

**What to build:**
Criar a entidade `Board`, repositório e endpoints CRUD em `/api/boards` para o usuário autenticado, com suporte a Soft Delete e criação automática do quadro `"Meu Primeiro Quadro"` com tarefas de exemplo quando um usuário novo for detectado.

**Blocked by:** 02: Estrutura de Segurança, Dev Mock e Sincronização JIT de Usuário

**Status:** done ✅
**Commit:** `f5e2ba9`

- [x] Criar entidade `Board` (`id`, `user_id`, `name`, `description`, timestamps, `deleted_at`) e `BoardRepository`.
- [x] Implementar `BoardService` e `BoardDTO` para listagem, criação, atualização e soft delete de quadros.
- [x] Implementar lógica de onboarding criando `"Meu Primeiro Quadro"` com 3 tarefas de boas-vindas no primeiro acesso do usuário.
- [x] Implementar `BoardController` com documentação OpenAPI.
- [x] Testes de integração cobrindo isolamento de quadros por usuário e comportamento de onboarding.
