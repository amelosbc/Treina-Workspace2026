# 04: Cartões do Kanban — Criação e Visualização Agrupada

**What to build:**
Permitir a criação de tarefas vinculadas a um quadro específico e retornar a visão do quadro Kanban com as tarefas agrupadas por suas colunas de status (`TO_DO`, `IN_PROGRESS`, `IN_REVIEW`, `DONE`), atribuindo automaticamente a próxima posição disponível na coluna.

**Blocked by:** 03: Gerenciamento de Quadros (Boards) e Onboarding Automático

**Status:** ready-for-agent

- [ ] Evoluir a entidade `Task` para conter `board_id`, `status` (`TaskStatus`), `priority` (`TaskPriority`), `dueDate`, `position` e `deleted_at`.
- [ ] Implementar endpoint `POST /api/boards/{boardId}/tasks` que calcula a posição final da coluna e salva.
- [ ] Implementar endpoint `GET /api/boards/{boardId}` retornando detalhes do quadro e mapa de tarefas por status ordenadas por `position`.
- [ ] Implementar endpoint `GET /api/boards/{boardId}/tasks` com filtros opcionais por status e prioridade.
- [ ] Testes de validação de dados e relacionamento `Board` ↔ `Task`.
